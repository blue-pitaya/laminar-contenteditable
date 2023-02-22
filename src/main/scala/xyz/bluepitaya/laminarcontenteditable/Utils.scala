package xyz.bluepitaya.laminarcontenteditable

object Utils {
  implicit class RichString(s: String) {
    def insertOnPos(pos: Int, text: String) = s.substring(0, pos) + text +
      s.substring(pos)
  }
}
