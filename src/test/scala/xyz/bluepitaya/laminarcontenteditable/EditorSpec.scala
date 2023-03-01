package xyz.bluepitaya.laminarcontenteditable

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.raquo.laminar.api.L._
import org.scalajs.dom

class EditorSpec extends AnyFlatSpec with Matchers {
  val highlighRedTextTransform = (v: String) => {
    val regex = """red""".r
    regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
  }

  def createWrapper() = {
    val wrapper = dom
      .document
      .createElement("DIV")
      .asInstanceOf[dom.HTMLElement]

    wrapper.setAttribute("id", "app")
    dom.document.querySelector("body").appendChild(wrapper)

    wrapper
  }

  "changing text externally" should "leave caret in same place" in {
    val text1 = "red tomato"
    val text2 = "red banana"

    val wrapper = createWrapper()

    val text = Var(text1)
    val app = Editor
      .component(
        Editor.Options(parseText = highlighRedTextTransform, text = text)
      )
      .amend(idAttr("editor"))
    render(wrapper, app)

    val editorElement = dom
      .document
      .getElementById("editor")
      .asInstanceOf[dom.HTMLElement]

    val caretPos = CaretPosition(4, 0)

    CaretOps.setCaretPosition(caretPos, editorElement)
    text.set(text2)

    CaretOps.getPosition(editorElement) shouldEqual Some(caretPos)
  }

  "changing text externally when caret is after new text" should
    "set caret to end of text" in {
      val text1 = "red tomato\n and red pinacolada"
      val text2 = "red banana"

      val wrapper = createWrapper()

      val text = Var(text1)
      val app = Editor
        .component(
          Editor.Options(parseText = highlighRedTextTransform, text = text)
        )
        .amend(idAttr("editor"))
      render(wrapper, app)

      val editorElement = dom
        .document
        .getElementById("editor")
        .asInstanceOf[dom.HTMLElement]

      CaretOps.setCaretPosition(CaretPosition(18, 0), editorElement)
      text.set(text2)

      CaretOps.getPosition(editorElement) shouldEqual Some(CaretPosition(11, 0))
    }
}
