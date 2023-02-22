package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.common.Hsv
import xyz.bluepitaya.laminarcontenteditable.Editor
import xyz.bluepitaya.laminarcontenteditable.Proof
import xyz.bluepitaya.laminarcontenteditable.LamEditor

object Main extends App {
  val f = (v: String) => {
    val regex = """red""".r
    regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
  }

  val app = div(
    Editor.componentWithDefaultStyles(f)
//    pre(
//      child.text <--
//        currentText
//          .signal
//          .map(v =>
//            if (v.isEmpty()) "Write \"red\" to see effect."
//            else v
//          )
//    )
  )

  val app2 = LamEditor.component()
  val app3 = Proof.component

  val containerNode = dom.document.querySelector("#app")

  render(containerNode, app3)
}
