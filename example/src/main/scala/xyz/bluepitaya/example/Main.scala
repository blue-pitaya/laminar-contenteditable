package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.common.Hsv
import xyz.bluepitaya.laminarcontenteditable.Editor

object Main extends App {
  val f = (v: String) => {
    val regex = """red""".r
    regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
  }

  val app = div(Editor.componentWithDefaultStyles(f))

  val containerNode = dom.document.querySelector("#app")

  render(containerNode, app)
}
