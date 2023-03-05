package xyz.bluepitaya.laminarcontenteditable

object StringHelper {
  implicit class RichString(s: String) {
    def insertOnCaret(caretPos: CaretPosition, text: String) = s
      .substring(0, caretPos.pos) + text +
      s.substring(caretPos.pos + caretPos.extent)

    /* Return indent size on line where caret resides */
    def getIndentSize(caretPosition: Int, indentChar: Char): Int = {
      def f(
          text: String,
          currentIndent: Int,
          countIndent: Boolean,
          pos: Int
      ): Int = text.headOption match {
        // pos < caretPosition for more elegant behavior
        case Some(ch)
            if countIndent && ch == indentChar && pos < caretPosition =>
          f(text.tail, currentIndent + 1, true, pos + 1)
        case Some(ch) if ch == '\n' =>
          if (pos >= caretPosition) currentIndent
          else f(text.tail, 0, true, pos + 1)
        case Some(ch) => f(text.tail, currentIndent, false, pos + 1)
        case None     => currentIndent
      }

      f(s, 0, true, 0)
    }
  }
}
