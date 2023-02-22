package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Proof {

  def component = {
    val col = Var("red")
    val text: Var[Seq[Node]] =
      Var(Seq(span(color <-- col, "color"), " is fine"))

    val refi = Var[Option[dom.HTMLElement]](None)

    val el = div(
      textArea(
        onMountCallback { ctx =>
          refi.set(Some(ctx.thisNode.ref))
        },
        onKeyDown -->
          Observer[dom.KeyboardEvent] { e =>
            dom.console.log(e)
          },
        onKeyUp -->
          Observer[dom.KeyboardEvent] { e =>
            dom.console.log(e)
          }
      ),
      pre(
        contentEditable(true),
        children <-- text,
        onKeyDown -->
          Observer[dom.KeyboardEvent] { e =>
            val ev = new dom.KeyboardEvent(
              e.`type`,
              e.asInstanceOf[dom.KeyboardEventInit]
            )
            refi.now().foreach(el => el.dispatchEvent(ev))
          },
        onKeyUp -->
          Observer[dom.KeyboardEvent] { e =>
            val ev = new dom.KeyboardEvent(
              e.`type`,
              e.asInstanceOf[dom.KeyboardEventInit]
            )
            refi.now().foreach(el => el.dispatchEvent(ev))
          }
      )
    )

    div(button("Change color", onClick.mapTo("blue") --> col), el)
  }
}
