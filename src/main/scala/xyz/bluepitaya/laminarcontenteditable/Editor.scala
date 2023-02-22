package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import com.raquo.domtypes.generic.nodes
import Utils.RichString
import MutationObserverHelper._

//FIXME: enter on blank text is broken

object Editor {
  private def flushChanges(
      parseText: String => String,
      state: Var[State],
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    println("change detected!")
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    state.update(s => s.updateText(textContent))
    // Aply user defined transform
    val parsedTextContent = parseText(textContent)

    val caretPosition = CaretOps.getPosition(element)
    commitChanges(caretPosition, parsedTextContent, element, observer)
  }

  private def insertTextOnCaret(
      text: String,
      parseText: String => String,
      state: Var[State],
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    // Aply user defined transform

    val caretPosition = CaretOps.getPosition(element)
    val updatedText = textContent.insertOnPos(caretPosition.pos, text)
    state.update(s => s.copy(text = updatedText))
    val parsedTextContent = parseText(updatedText)
    // TODO: what if parsed text is different than text?
    val updatedCaret = caretPosition.copy(pos = caretPosition.pos + text.size)

    commitChanges(updatedCaret, parsedTextContent, element, observer)
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
    CaretOps.setCaretPosition(element, caretPosition)
    observer.observeElement(element)
  }

  val styles = Seq(
    padding("10px"),
    border("1px solid black"),
    width("500px"),
    height("500px"),
    overflowY.auto
  )

  private def getIndentSize(
      text: String,
      caretPosition: Int,
      indentChar: Char
  ): Int = {
    def f(
        text: String,
        currentIndent: Int,
        countIndent: Boolean,
        pos: Int
    ): Int = text.headOption match {
      // pos < caretPosition for more elegant behavior
      case Some(ch) if countIndent && ch == indentChar && pos < caretPosition =>
        f(text.tail, currentIndent + 1, true, pos + 1)
      case Some(ch) if ch == '\n' =>
        if (pos >= caretPosition) currentIndent
        else f(text.tail, 0, true, pos + 1)
      case Some(ch) => f(text.tail, currentIndent, false, pos + 1)
      case None     => currentIndent
    }

    f(text, 0, true, 0)
  }

  // FIXME: traversal of pre element first child only
  // TODO: hitting enter halfway of indent will leave incomplete indent on original line
  // TODO: current text can be returned as EventStream or Signal
  /** Do not amend any new elements to this components */
  def component(parseText: String => String) = {

    val state = Var(State("", CaretPosition(0, 0)))

    val textSignal = state.signal.map(_.text)
    val caretSignal = state.signal.map(_.caretPosition)

    div(
      pre(
        contentEditable(true),
        whiteSpace.preWrap,
        width("100%"),
        height("100%"),
        margin("0"),
        outline("none"),
        // TODO: mutation observer may be not working properly when not in onMount cb
        inContext { ctx =>
          val element = ctx.ref
          val mutationObserver = new dom.MutationObserver((_, mutObs) => {
            flushChanges(parseText, state, element, mutObs)
          })
          mutationObserver.observeElement(element)

          Seq(
            onKeyDown -->
              autoTabsObserver(state, parseText, element, mutationObserver)
          )
        }
      )
    )
  }

  def componentWithDefaultStyles(parseText: String => String) = {
    component(parseText).amend(styles)
  }

  private def autoTabsObserver(
      state: Var[State],
      parseText: String => String,
      element: dom.HTMLElement,
      mutationObserver: dom.MutationObserver
  ) = {
    val indentChar = '\t'

    Observer[dom.KeyboardEvent] { e =>
      if (e.keyCode == dom.KeyCode.Tab) {
        e.preventDefault()
        insertTextOnCaret(
          indentChar.toString(),
          parseText,
          state,
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
        val indentSize = getIndentSize(text, position, indentChar)
        insertTextOnCaret(
          "\n" + (indentChar.toString * indentSize),
          parseText,
          state,
          element,
          mutationObserver
        )
      }
    }
  }
}
