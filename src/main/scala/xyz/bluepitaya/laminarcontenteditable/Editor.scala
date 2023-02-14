package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

//FIXME: enter on blank text is broken

object Editor {
  private val observerConfig = new dom.MutationObserverInit {
    characterData = true;
    characterDataOldValue = true;
    childList = true;
    subtree = true;
  }

  private def observeChanges(
      element: dom.HTMLElement,
      mutationObserer: dom.MutationObserver
  ): Unit = {
    mutationObserer.observe(element, observerConfig)
  }

  private def setCaretPosition(
      caretPosition: CaretPosition,
      element: dom.HTMLElement
  ): Unit = {
    val range = CaretOps.makeRange(element, caretPosition)
    CaretOps.setCurrentRange(range)
  }

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
    setCaretPosition(caretPosition, element)
    observeChanges(element, observer)

  }

  private def insertOnPos(original: String, pos: Int, appendedText: String) =
    original.substring(0, pos) + appendedText + original.substring(pos)

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
    val updatedText = insertOnPos(textContent, caretPosition.pos, text)
    textState.set(updatedText)
    val parsedTextContent = parseText(updatedText)
    val updatedCaret = caretPosition.copy(pos = caretPosition.pos + text.size)

    commitChanges(updatedCaret, parsedTextContent, element, observer)
  }

  private def createMutationObserver(
      parseText: String => String,
      textState: Var[String],
      element: dom.HTMLElement
  ) = new dom.MutationObserver((_, mutObs) => {
    flushChanges(parseText, textState, element, mutObs)
  })

  val styles = Seq(
    padding("10px"),
    border("1px solid black"),
    width("500px"),
    height("500px"),
    overflowY.auto
  )

  def getIndentSize(
      text: String,
      caretPosition: Int,
      indentChar: Char = ' '
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
  def component(currentText: Var[String], parseText: String => String) = {
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
          val mutationObserver =
            createMutationObserver(parseText, currentText, element)
          observeChanges(element, mutationObserver)

          onKeyDown -->
            Observer[dom.KeyboardEvent] { e =>
              if (e.keyCode == dom.KeyCode.Tab) {
                e.preventDefault()
                insertTextOnCaret(
                  "  ",
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
                val indentSize = getIndentSize(text, position)
                insertTextOnCaret(
                  "\n" + (" " * indentSize),
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
