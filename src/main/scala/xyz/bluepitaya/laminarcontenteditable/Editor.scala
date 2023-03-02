package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import MutationObserverHelper._
import StringHelper._

object Editor {
  case class Options(
      parseText: String => String,
      autoIndent: Signal[Boolean] = Val(false),
      autoIndentChar: Char = '⊕',
      bus: EventBus[Event] = new EventBus[Event]
  )

  sealed trait Event
  case class SetText(v: String) extends Event
  case class TextChanged(v: String) extends Event

  private[laminarcontenteditable] sealed trait Ev
  private[laminarcontenteditable] case class ChangeHtml(
      v: String,
      caretPosition: Option[CaretPosition]
  ) extends Ev
  private[laminarcontenteditable] case object MutObsOn extends Ev
  private[laminarcontenteditable] case object MutObsOff extends Ev

  def component(options: Options) = {
    val evBus: EventBus[Ev] = new EventBus
    val keyBus: EventBus[dom.KeyboardEvent] = new EventBus

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

            commitTextChange(textContent, caretPosition, evBus, options)
          })

          evBus --> evObserver(mutationObserver, element)
        },
        onMountCallback { _ =>
          evBus.emit(MutObsOn)
        },
        inContext { ctx =>
          Seq(
            options.bus --> obs(ctx.ref, evBus, options),
            onKeyDown --> keyBus,
            keyBus.events.withCurrentValueOf(options.autoIndent) --> {
              case (e, true) => AutoIndent
                  .onKeyDownObserver(e, options, ctx.ref, evBus)
              case (e, false) => ()
            }
          )
        }
      )
    )
  }

  def componentWithDefaultStyles(options: Options) = {
    val styles = Seq(
      padding("10px"),
      border("1px solid black"),
      width("500px"),
      height("500px"),
      overflowY.auto
    )

    component(options).amend(styles)
  }

  private def obs(
      element: dom.HTMLElement,
      evBus: EventBus[Ev],
      options: Options
  ): Observer[Event] = Observer[Event] { e =>
    e match {
      case SetText(v) =>
        val caretPosition = CaretOps.getPosition(element)
        commitTextChange(v, caretPosition, evBus, options)
      case TextChanged(v) => ()
    }
  }

  private def evObserver(
      mutObs: dom.MutationObserver,
      element: dom.HTMLElement
  ) = Observer[Ev] { e =>
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
      evBus: EventBus[Ev],
      options: Options
  ): Unit = {
    val escapedHtml = HtmlEscape.escape(text)
    val parsedTextContent = options.parseText(escapedHtml)
    // Turn content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(parsedTextContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    evBus.emit(MutObsOff)
    evBus.emit(ChangeHtml(htmlContent, caretPosition))
    evBus.emit(MutObsOn)
    options.bus.emit(TextChanged(text))
  }

  private[laminarcontenteditable] def insertTextOnCaret(
      text: String,
      options: Options,
      element: dom.HTMLElement,
      evBus: EventBus[Ev]
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    val caretPosition = CaretOps.getPosition(element)

    caretPosition.foreach { caretPosition =>
      val updatedText = textContent.insertOnPos(caretPosition.pos, text)
      // TODO: always good caret?
      val updatedCaret = caretPosition.copy(pos = caretPosition.pos + text.size)

      commitTextChange(updatedText, Some(updatedCaret), evBus, options)
    }
  }
}
