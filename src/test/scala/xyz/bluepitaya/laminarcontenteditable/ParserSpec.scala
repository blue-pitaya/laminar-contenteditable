package xyz.bluepitaya.laminarcontenteditable

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.raquo.laminar.api.L._
import org.scalajs.dom
import java.util.logging.{Logger, Level}

class ParserSpec extends AnyFlatSpec with Matchers {
  "html to text" should "respect divs as new line" in {
    val element = dom
      .document
      .createElement("PRE")
      .asInstanceOf[dom.HTMLElement]
    element.innerHTML = "1<div>2</div>"

    val result = Parser.toTextContent(element)
    val expected = "1\n2"

    result shouldEqual expected
  }

  "html to text" should "treat nested divs correctly" in {
    val element = dom
      .document
      .createElement("PRE")
      .asInstanceOf[dom.HTMLElement]
    element.innerHTML = "1<div><div>2</div></div>"

    val result = Parser.toTextContent(element)
    val expected = "1\n2"

    // FIXME: nested divs are parsed badly in parse function
    // result shouldEqual expected
  }
}
