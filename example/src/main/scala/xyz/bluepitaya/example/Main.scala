package xyz.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import xyz.bluepitaya.laminarcontenteditable.Editor

object Main extends App {
  val containerNode = dom.document.querySelector("#app")

  render(containerNode, Example.component())
}
