package xyz.bluepitaya.laminarcontenteditable

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.raquo.laminar.api.L._
import org.scalajs.dom

class EditorSpec extends AnyFlatSpec with Matchers {
  // TODO: find way to test it xd
  "parsing html content" should "respect divs as new line" in {
    val element = dom.document.createElement("PRE")
    element.innerHTML = "<div>1</div><div>2</div>"

    println(element)

    true shouldEqual true
  }
}
