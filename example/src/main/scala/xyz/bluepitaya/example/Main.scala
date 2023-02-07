package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.common.Hsv
import xyz.bluepitaya.laminarcontenteditable.ContentEditable
import xyz.bluepitaya.laminarcontenteditable.ConEd

object Main extends App {
  val state = Var(ContentEditable.State(None))
  val app = div(
    pre(
      whiteSpace.preWrap,
      border("1px solid black"),
      width("500px"),
      height("500px"),
      contentEditable(true),
      inContext { ctx =>
        val evts = ContentEditable
          .events(state, ctx.ref, ContentEditable.Options(Some(2)))
        ctx.ref.innerHTML = """<span style="color: red;">elo</span> chuj"""
        evts
      }
    )
  )

  val app2 = div(ConEd.comp)

  val containerNode = dom.document.querySelector("#app")

  render(containerNode, app2)
}
