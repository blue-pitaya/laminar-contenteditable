package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.laminarcontenteditable.Editor

object Main extends App {
  val text = Var("red tomato")

  val editorOptions = {
    val f = (v: String) => {
      val regex = """red""".r
      regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
    }

    Editor.Options(parseText = f, text = text, autoIndent = Val(true))
  }

  val app = div(
    Editor.componentWithDefaultStyles(editorOptions),
    button("Write red banana", onClick.mapTo("red banana") --> text),
    pre(
      child.text <--
        text
          .signal
          .map(v =>
            if (v.isEmpty()) "Write \"red\" to see effect."
            else v
          )
    )
  )

  val containerNode = dom.document.querySelector("#app")

  render(containerNode, app)
}
