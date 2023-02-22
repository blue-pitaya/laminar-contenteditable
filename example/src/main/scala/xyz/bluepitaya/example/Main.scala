package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.common.Hsv
import xyz.bluepitaya.laminarcontenteditable.Editor

object Main extends App {
  val text = Var("")

  val editorOptions = {
    val f = (v: String) => {
      val regex = """red""".r
      regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
    }

    Editor.Options(parseText = f, onTextChanged = x => text.set(x))
  }

  val app = div(
    Editor.componentWithDefaultStyles(editorOptions),
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
