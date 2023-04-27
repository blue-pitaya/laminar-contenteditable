package dev.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import dev.bluepitaya.laminarcontenteditable.Editor

object Main extends App {
  val containerNode = dom.document.querySelector("#app")

  render(containerNode, Example.component())
}
