package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Proof {

  def component = {
    val col = Var("red")
    val text: Var[Seq[Node]] =
      Var(Seq(span(color <-- col, "color"), " is fine"))

    val el = div(
      pre(
        contentEditable(true),
        children <-- text,
        onKeyDown -->
          Observer[Any] { _ =>
            val textVal = text.now()
            println(textVal)
          }
      )
    )

    div(button("Change color", onClick.mapTo("blue") --> col), el)
  }
}
