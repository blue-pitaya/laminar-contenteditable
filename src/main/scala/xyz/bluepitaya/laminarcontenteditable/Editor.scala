package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

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
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    // Aply user defined transform
    val parsedTextContent = parseText(textContent)
    // Turn back content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(parsedTextContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    observer.disconnect()
    val caretPosition = CaretOps.getPosition(element)
    element.innerHTML = htmlContent
    // Restore caret position which was resetted with change of inner HTML
    setCaretPosition(caretPosition, element)
    observeChanges(element, observer)
  }

  private def createMutationObserver(
      parseText: String => String,
      element: dom.HTMLElement
  ) = new dom.MutationObserver((_, mutObs) => {
    flushChanges(parseText, element, mutObs)
  })

  val styles = Seq(
    whiteSpace.preWrap,
    outline("none"),
    padding("10px"),
    border("1px solid black"),
    width("500px"),
    height("500px"),
    contentEditable(true)
  )

  def component(parseText: String => String) = {
    pre(
      onMountCallback { ctx =>
        val element = ctx.thisNode.ref
        val mutationObserver = createMutationObserver(parseText, element)
        observeChanges(element, mutationObserver)
      }
    )
  }

  def componentWithDefaultStyles(parseText: String => String) = {
    component(parseText).amend(styles)
  }
}
