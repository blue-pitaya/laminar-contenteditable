package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import MutationObserverHelper._
import StringHelper._

object Editor {
  case class Options(
      parseText: String => String = identity,
      textSignal: Signal[String] = Signal.fromValue(""),
      onTextChanged: Observer[String] = Observer[String](_ => ()),
      autoIndent: Signal[Boolean] = Val(false),
      autoIndentChar: Char = '\t'
  )

  private[laminarcontenteditable] sealed trait Event
  private[laminarcontenteditable] case class ChangeHtml(
      v: String,
      caretPosition: Option[CaretPosition]
  ) extends Event
  private[laminarcontenteditable] case object MutObsOn extends Event
  private[laminarcontenteditable] case object MutObsOff extends Event

  def component(options: Options = Editor.Options()) = {
    val evBus: EventBus[Event] = new EventBus
    val textUpdateBus: EventBus[String] = new EventBus

    div(
      pre(
        contentEditable(true),
        whiteSpace.preWrap,
        width("100%"),
        height("100%"),
        margin("0"),
        outline("none"),
        onMountBind { ctx =>
          val element = ctx.thisNode.ref
          val mutationObserver = new dom.MutationObserver((_, mutObs) => {
            // Get text content with newlines from element
            val textContent = Parser.toTextContent(element)
            val caretPosition = CaretOps.getPosition(element)

            commitTextChange(
              textContent,
              caretPosition,
              evBus,
              textUpdateBus,
              options
            )
          })

          evBus --> evObserver(mutationObserver, element)
        },
        onMountCallback { _ =>
          evBus.emit(MutObsOn)
        },
        inContext { ctx =>
          Seq(
            options.textSignal --> { v =>
              val caretPosition = CaretOps.getPosition(ctx.ref)
              commitTextChange(
                v,
                caretPosition,
                evBus,
                textUpdateBus,
                options,
                // don't update text state to avoid infinite loop
                updateText = false
              )
            },
            onKeyDown.compose(
              _.withCurrentValueOf(options.autoIndent)
                .collect { case (event, true) =>
                  event
                }
            ) -->
              AutoIndent
                .onKeyDownObserver(options, ctx.ref, evBus, textUpdateBus)
          )
        },
        textUpdateBus --> options.onTextChanged
      )
    )
  }

  private def evObserver(
      mutObs: dom.MutationObserver,
      element: dom.HTMLElement
  ) = Observer[Event] { e =>
    e match {
      case ChangeHtml(v, caretPosition) =>
        element.innerHTML = v
        // Restore caret position which was resetted with change of inner HTML
        caretPosition.foreach(v => CaretOps.setCaretPosition(v, element))
      case MutObsOn  => mutObs.observeElement(element)
      case MutObsOff => mutObs.disconnect()
    }
  }

  private def commitTextChange(
      text: String,
      caretPosition: Option[CaretPosition],
      evBus: EventBus[Event],
      textUpdateBus: EventBus[String],
      options: Options,
      updateText: Boolean = true
  ): Unit = {
    val escapedHtml = HtmlEscape.escape(text)
    val parsedTextContent = options.parseText(escapedHtml)
    // Turn content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(parsedTextContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    evBus.emit(MutObsOff)
    evBus.emit(ChangeHtml(htmlContent, caretPosition))
    evBus.emit(MutObsOn)
    if (updateText) {
      textUpdateBus.emit(text)
    }
  }

  private[laminarcontenteditable] def insertTextOnCaret(
      text: String,
      options: Options,
      element: dom.HTMLElement,
      evBus: EventBus[Event],
      textUpdateBus: EventBus[String]
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    val caretPosition = CaretOps.getPosition(element)

    caretPosition.foreach { caretPosition =>
      val updatedText = textContent.insertOnCaret(caretPosition, text)
      // TODO: always good caret?
      val updatedCaret = caretPosition.copy(pos = caretPosition.pos + text.size)

      commitTextChange(
        updatedText,
        Some(updatedCaret),
        evBus,
        textUpdateBus,
        options
      )
    }
  }
}
