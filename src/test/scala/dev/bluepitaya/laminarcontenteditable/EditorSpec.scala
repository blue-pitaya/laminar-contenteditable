package dev.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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

  def createSut(wrapper: dom.HTMLElement, text: Var[String]) = {
    val app = Editor
      .component(
        Editor.Options(
          parseText = highlighRedTextTransform,
          textSignal = text.signal,
          onTextChanged = text.writer
        )
      )
      .amend(idAttr("editor"))
    render(wrapper, app)
  }

  def editorElement = dom
    .document
    .getElementById("editor")
    .asInstanceOf[dom.HTMLElement]

  "changing text externally" should "leave caret in same place" in {
    val text1 = "red tomato"
    val text2 = "red banana"

    val text = Var(text1)

    createSut(createWrapper(), text)

    CaretOps.setCaretPosition(CaretPosition(4, 0), editorElement)
    text.set(text2)

    CaretOps.getPosition(editorElement) shouldEqual Some(CaretPosition(4, 0))
  }

  "changing text externally when caret is after new text" should
    "set caret to end of text" in {
      val text1 = "red tomato\n and red pinacolada"
      val text2 = "red banana"

      val text = Var(text1)

      createSut(createWrapper(), text)

      CaretOps.setCaretPosition(CaretPosition(18, 0), editorElement)
      text.set(text2)

      CaretOps.getPosition(editorElement) shouldEqual Some(CaretPosition(11, 0))
    }
}
