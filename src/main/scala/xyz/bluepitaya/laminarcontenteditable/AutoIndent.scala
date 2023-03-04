package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import StringHelper._

object AutoIndent {
  def onKeyDownObserver(
      e: dom.KeyboardEvent,
      options: Editor.Options,
      element: dom.HTMLElement,
      evBus: EventBus[Editor.Event]
  ): Unit = {
    val indentChar = options.autoIndentChar

    if (e.keyCode == dom.KeyCode.Tab) {
      e.preventDefault()
      Editor.insertTextOnCaret(indentChar.toString(), options, element, evBus)
    }

    if (e.keyCode == dom.KeyCode.Enter) {
      e.preventDefault()
      // get indent on current line
      val caretPosition = CaretOps.getPosition(element)
      caretPosition.foreach { caretPosition =>
        // we don't care about extend, because it will be deleted anyway be pressing enter
        val position = caretPosition.pos
        val text = Parser.toTextContent(element)
        val indentSize = text.getIndentSize(position, indentChar)
        Editor.insertTextOnCaret(
          "\n" + (indentChar.toString * indentSize),
          options,
          element,
          evBus
        )
      }
    }
  }
}
