package dev.bluepitaya.example

import com.raquo.laminar.api.L._
import org.scalajs.dom
import dev.bluepitaya.laminarcontenteditable.Editor

object Example {
  def component() = {
    val text =
      Var("""Some hex colors (try write one yourself): #FF0000 #00aa00 #0000ff
    |	Auto indent included. (hit enter on this line)
    |
    |Text in quotes is 'big'!!!""".stripMargin)

    val editorOptions = Editor.Options(
      parseText = (text: String) => {
        // special HTML characters (like single quote) are escaped
        val quoteRegex = """&#39;(.+?)&#39;""".r
        val halfParsedText = quoteRegex.replaceAllIn(
          text,
          m => s"""<span style="font-size: 20px;">'${m.group(1)}'</span>"""
        )
        val colorRegex = """#([0-9]|[a-f]|[A-F]){6}""".r
        val parsedText = colorRegex.replaceAllIn(
          halfParsedText,
          m => s"""<span style="color: $m;">$m</span>"""
        )

        parsedText
      },
      autoIndent = Val(true),
      textSignal = text.signal,
      onTextChanged = text.writer
    )

    val editorStyles = Seq(
      padding("10px"),
      border("1px solid black"),
      width("600px"),
      height("300px"),
      overflowY.auto
    )

    div(
      display.flex,
      flexDirection.row,
      styleProp("gap")("30px"),
      div(
        h2("Extended textarea"),
        Editor.component(editorOptions).amend(editorStyles)
      ),
      div(h2("Raw text"), pre(child.text <-- text.signal))
    )
  }
}
