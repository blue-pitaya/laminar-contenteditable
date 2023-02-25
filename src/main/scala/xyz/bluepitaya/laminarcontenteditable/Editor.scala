package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import MutationObserverHelper._
import StringHelper._
import io.laminext.syntax.dangerous._

//FIXME: enter on blank text is broken

object Editor {
  case class Options(parseText: String => String, text: Var[String])

  sealed trait Ev
  case class ChangeHtml(v: String, caretPosition: Option[CaretPosition])
      extends Ev
  case object MutObsOn extends Ev
  case object MutObsOff extends Ev

  // FIXME: traversal of pre element first child only
  // TODO: hitting enter halfway of indent will leave incomplete indent on original line
  // TODO: current text can be returned as EventStream or Signal
  /** Do not amend any new elements to this components, see "innerHTML warning"
    * in README for more details.
    */
  def component(options: Options) = {
    val evBus: EventBus[Ev] = new EventBus

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
            options.text.set(textContent)
          })

          evBus --> evObserver(mutationObserver, element)
        },
        onMountCallback { _ =>
          evBus.emit(MutObsOn)
        },
        inContext { ctx =>
          options.text -->
            Observer[String](v => onTextChange(v, options, ctx.ref, evBus))
        }
        // onMountBind { ctx =>
        //  val element = ctx.thisNode.ref
        //  val mutationObserver = new dom.MutationObserver((_, mutObs) => {
        //    flushChanges(options, element, mutObs)
        //  })
        //  mutationObserver.observeElement(element)

        //  onKeyDown -->
        //    AutoIndent.onKeyDownObserver(element, mutationObserver, options)
        // }
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

  private def commit(
      text: String,
      caretPosition: Option[CaretPosition],
      evBus: EventBus[Ev],
      options: Options
  ): Unit = {
    val parsedTextContent = options.parseText(text)
    // Turn content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(parsedTextContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    evBus.emit(MutObsOff)
    // FIXME: HTML SANITAZATION TO PREVENT XSS
    evBus.emit(ChangeHtml(htmlContent, caretPosition))
    evBus.emit(MutObsOn)
  }

  private def onTextChange(
      text: String,
      options: Options,
      element: dom.HTMLElement,
      evBus: EventBus[Ev]
  ): Unit = {
    val caretPosition = CaretOps.getPosition(element)

    commit(text, caretPosition, evBus, options)
  }

  def insertTextOnCaret(
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

      // Aply user defined transform
      val updatedCaret =
        Some(caretPosition.copy(pos = caretPosition.pos + text.size))

      commit(updatedText, updatedCaret, evBus, options)
    }
  }

}
