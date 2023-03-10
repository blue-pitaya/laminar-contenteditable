package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.laminarcontenteditable.Editor

object Main extends App {
  val text = Var("red tomato")
  val textChangeSignal = text
    .signal
    .map { v =>
      if (v.text.isEmpty()) "Write \"red\" to see effect."
      else v.text
    }

  val editorOptions = {
    val f = (v: String) => {
      val regex = """red""".r
      regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
    }

    Editor.Options(
      parseText = f,
      autoIndent = Val(true),
      textSignal = text.signal,
      onTextChanged = text.writer
    )
  }

  val buttText = "red banana"
  val buttText2 = "<h1>ElO!</h1><h2>YO!</h2>"
  val buttText3 = "<div><div>1</div><div>2</div></div>"

  val app = div(
    Editor.componentWithDefaultStyles(editorOptions),
    button("Write red banana", onClick.mapTo(buttText) --> text),
    pre(child.text <-- textChangeSignal),
    h3("Normal content editable div"),
    div(
      width("300px"),
      height("300px"),
      padding("10px"),
      backgroundColor("#dddddd"),
      contentEditable(true),
      onMountCallback { ctx =>
        ctx.thisNode.ref.innerHTML = buttText2
      }
    )
  )

  val containerNode = dom.document.querySelector("#app")

  render(containerNode, app)
}
