package dev.bluepitaya.laminarcontenteditable

object HtmlEscape {
  def escape(html: String): String = {
    val regex = """["'&<>]""".r

    regex.replaceAllIn(
      html,
      m =>
        m.matched match {
          case "\"" => "&quot;"
          case "&"  => "&amp;"
          case "'"  => "&#39;"
          case "<"  => "&lt;"
          case ">"  => "&gt;"
          case s    => s
        }
    )
  }
}
