package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Editor {
  val observerConfig = new dom.MutationObserverInit {
    characterData = true;
    characterDataOldValue = true;
    childList = true;
    subtree = true;
  }

  def observeChanges(
      element: dom.HTMLElement,
      mutationObserer: dom.MutationObserver
  ): Unit = {
    mutationObserer.observe(element, observerConfig)
  }

  def setCaretPosition(
      caretPosition: CaretPosition,
      element: dom.HTMLElement
  ): Unit = {
    val range = CaretOps.makeRange(element, caretPosition)
    CaretOps.setCurrentRange(range)
  }

  def flushChanges(
      parseText: String => String,
      element: dom.HTMLElement,
      observer: dom.MutationObserver
  ): Unit = {
    // Get text content with newlines from element
    val textContent = Parser.toTextContent(element)
    println("***")
    println(textContent)
    // Aply user defined transform
    val parsedTextContent = parseText(textContent)
    // Turn back content to html content with respect for contenteditable logic
    val htmlContent = Parser.toHtmlContent(parsedTextContent)
    println("---")
    println(htmlContent)

    // Mutation observer must be disconnected before we manually change innerHTML to avoid infinite loop
    observer.disconnect()
    val caretPosition = CaretOps.getPosition(element)
    println(caretPosition)
    element.innerHTML = htmlContent
    // Restore caret position which was resetted with change of inner HTML
    setCaretPosition(caretPosition, element)
    observeChanges(element, observer)
  }

  def createMutationObserver(
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
      styles,
      onMountCallback { ctx =>
        val element = ctx.thisNode.ref
        element.innerHTML = ""
        val mutationObserver = createMutationObserver(parseText, element)
        observeChanges(element, mutationObserver)

      },
      onClick --> Observer[Any](_ => dom.console.log(CaretOps.getCurrentRange)),
      inContext { ctx =>
        onClick --> Observer[Any](_ => println(CaretOps.getPosition(ctx.ref)))
      }
    )
  }
}
