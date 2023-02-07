package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import java.util.concurrent.CopyOnWriteArraySet

object ConEd {

  def comp = {
    def onKeyDown(event: dom.KeyboardEvent, element: dom.HTMLElement): Unit = {
      if (event.key == "Enter") {
        event.preventDefault()
        // Firefox Quirk: Since plaintext-only is unsupported we must
        // ensure that only newline characters are inserted
        val position = ContentEditable.getPosition(element)
        // We also get the current line and preserve indentation for the next
        // line that's created
        val notWhitespaceRegex = """\S""".r
        val index = notWhitespaceRegex
          .findFirstMatchIn(position.content)
          .map(x => x.start)
          .getOrElse(0)
        val text = "\n" + position.content.slice(0, index)
        ContentEditable.insert(text, None, element, position)
      }
    }

    def onKeyUp(event: dom.KeyboardEvent, element: dom.HTMLElement): Unit = {
      val pos = ContentEditable.getPosition(element)

      val content = ContentEditable.htmlElementToString(element)
      println(content)
      val nextContent = content
        .split("\n", -1)
        .map(line => "<div>" + line + "</div>")
        .mkString("\n")
      println(nextContent)

      // TODO: regex for windows \r\n
      // element.innerHTML = nextContent

      // ContentEditable.setCurrentRange(
      //  Utils.makeRange(element, pos.position, pos.position + pos.extent)
      // )

      // Chrome Quirk: The contenteditable may lose focus after the first edit or so
      element.focus();
    }

    pre(
      whiteSpace.preWrap,
      outline("none"),
      padding("10px"),
      border("1px solid black"),
      width("500px"),
      height("500px"),
      contentEditable(true),
      inContext { ctx =>
        ctx.ref.innerHTML = "<div>1</div><div><br /></div>"
        Seq(
          // documentEvents.onKeyDown -->
          //  Observer { e =>
          //    onKeyDown(e, ctx.ref)
          //  },
          documentEvents.onKeyUp -->
            Observer { e =>
              onKeyUp(e, ctx.ref)
            },
          onClick -->
            Observer[Any] { _ =>
              ContentEditable.log(ContentEditable.getCurrentRange)
            }
        )
      }
    )
  }
}
