package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import MutationObserverHelper._
import StringHelper._

//FIXME: enter on blank text is broken

object Editor {
  private def flushChanges(
      parseText: String => String,
      textState: Var[String],
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    // Aply user defined transform
    val parsedTextContent = parseText(textContent)
    println(textContent)
    textState.set(textContent)

    val caretPosition = CaretOps.getPosition(element)
    commitChanges(caretPosition, parsedTextContent, element, observer)
  }

  private def commitChanges(
      caretPosition: CaretPosition,
      textContent: String,
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Turn content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(textContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    observer.disconnect()
    element.innerHTML = htmlContent
    // Restore caret position which was resetted with change of inner HTML
    CaretOps.setCaretPosition(caretPosition, element)
    observer.observeElement(element)
  }

  private def insertTextOnCaret(
      text: String,
      parseText: String => String,
      textState: Var[String],
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    // Aply user defined transform

    val caretPosition = CaretOps.getPosition(element)
    val updatedText = textContent.insertOnPos(caretPosition.pos, text)
    textState.set(updatedText)
    val parsedTextContent = parseText(updatedText)
    val updatedCaret = caretPosition.copy(pos = caretPosition.pos + text.size)

    commitChanges(updatedCaret, parsedTextContent, element, observer)
  }

  val styles = Seq(
    padding("10px"),
    border("1px solid black"),
    width("500px"),
    height("500px"),
    overflowY.auto
  )

  // FIXME: traversal of pre element first child only
  // TODO: hitting enter halfway of indent will leave incomplete indent on original line
  // TODO: current text can be returned as EventStream or Signal
  /** Do not amend any new elements to this components */
  def component(currentText: Var[String], parseText: String => String) = {
    val indentChar = '\t'

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
            flushChanges(parseText, currentText, element, mutObs)
          })
          mutationObserver.observeElement(element)

          onKeyDown -->
            Observer[dom.KeyboardEvent] { e =>
              if (e.keyCode == dom.KeyCode.Tab) {
                e.preventDefault()
                insertTextOnCaret(
                  indentChar.toString(),
                  parseText,
                  currentText,
                  element,
                  mutationObserver
                )
              }

              if (e.keyCode == dom.KeyCode.Enter) {
                e.preventDefault()
                // get indent on current line
                val caretPosition = CaretOps.getPosition(element)
                // we don't care about extend, because it will be deleted anyway be pressing enter
                val position = caretPosition.pos
                val text = Parser.toTextContent(element)
                val indentSize = text.getIndentSize(position, indentChar)
                insertTextOnCaret(
                  "\n" + (indentChar.toString * indentSize),
                  parseText,
                  currentText,
                  element,
                  mutationObserver
                )
              }
            }
        }
      )
    )
  }

  def componentWithDefaultStyles(
      currentText: Var[String],
      parseText: String => String
  ) = {
    component(currentText, parseText).amend(styles)
  }
}
