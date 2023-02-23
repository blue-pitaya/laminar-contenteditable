package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import MutationObserverHelper._
import StringHelper._
import io.laminext.syntax.dangerous._

//FIXME: enter on blank text is broken

object Editor {
  case class Options(parseText: String => String, onTextChanged: String => Unit)

  sealed trait Ev
  case class ChangeHtml(v: String) extends Ev
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
    val mutObs: Var[Option[dom.MutationObserver]] = Var(None)

    val htmlSignal = evBus
      .events
      .filter(e =>
        e match {
          case ChangeHtml(v) => true
          case _             => false
        }
      )
      .map(e =>
        e match {
          case ChangeHtml(v) => v
          case _             => ""
        }
      )
      .toSignal("")

    div(
      pre(
        contentEditable(true),
        whiteSpace.preWrap,
        width("100%"),
        height("100%"),
        margin("0"),
        outline("none"),
        unsafeInnerHtml <-- htmlSignal,
        inContext { ctx =>
          mutObs -->
            Observer[Option[dom.MutationObserver]](v =>
              v match {
                case None        => ()
                case Some(value) => value.observeElement(ctx.ref)
              }
            )
        },
        onMountCallback { ctx =>
          val element = ctx.thisNode.ref
          val mutationObserver = new dom.MutationObserver((_, mutObs) => {
            flushChanges(options, element, mutObs)
          })

          mutObs.set(Some(mutationObserver))
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

  private def flushChanges(
      options: Options,
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    val caretPosition = CaretOps.getPosition(element)

    commitChanges(textContent, caretPosition, options, element, observer)
  }

  def insertTextOnCaret(
      text: String,
      options: Options,
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    val caretPosition = CaretOps.getPosition(element)
    val updatedText = textContent.insertOnPos(caretPosition.pos, text)

    // Aply user defined transform
    val updatedCaret = caretPosition.copy(pos = caretPosition.pos + text.size)

    commitChanges(updatedText, updatedCaret, options, element, observer)
  }

  private def commitChanges(
      textContent: String,
      caretPosition: CaretPosition,
      options: Options,
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    val parsedTextContent = options.parseText(textContent)
    // Turn content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(parsedTextContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    observer.disconnect()
    // FIXME: HTML SANITAZATION TO PREVENT XSS
    element.innerHTML = htmlContent
    // Restore caret position which was resetted with change of inner HTML
    CaretOps.setCaretPosition(caretPosition, element)
    // On chage callback after we actaully updated real element
    // options.onTextChanged(textContent)
    observer.observeElement(element)
  }
}
