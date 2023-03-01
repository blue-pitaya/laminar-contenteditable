package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.laminarcontenteditable.Editor
import xyz.bluepitaya.laminarcontenteditable.Editor.SetText
import xyz.bluepitaya.laminarcontenteditable.Editor.TextChanged

object Main extends App {
  val bus = new EventBus[Editor.Event]
  val textChangeStream = bus
    .events
    .map { e =>
      e match {
        case SetText(v) => ""
        case TextChanged(v) =>
          if (v.text.isEmpty()) "Write \"red\" to see effect."
          else v.text
      }
    }

  val editorOptions = {
    val f = (v: String) => {
      val regex = """red""".r
      regex.replaceAllIn(v, _ => """<span style="color: red;">red</span>""")
    }

    val insertTextF = (v: String) => {
      "1<div><div>2</div></div>"
    }

    Editor.Options(parseText = f, autoIndent = Val(true), bus = bus)
  }

  val buttText = "red banana"
  val buttText2 = "<h1>ElO!</h1><h2>YO!</h2>"
  val buttText3 = "<div><div>1</div><div>2</div></div>"

  val app = div(
    Editor.componentWithDefaultStyles(editorOptions),
    button("Write red banana", onClick.mapTo(Editor.SetText(buttText)) --> bus),
    pre(child.text <-- textChangeStream),
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
  bus.emit(SetText("red tomato"))
}
