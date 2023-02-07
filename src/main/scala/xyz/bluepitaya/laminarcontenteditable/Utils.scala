package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Utils {
  import DomNodeExtensions.RichNode
  def setStart(range: dom.Range, node: dom.Node, offset: Int): Unit =
    if (offset < node.textContent.size) range.setStart(node, offset)
    else range.setStartAfter(node)

  def setEnd(range: dom.Range, node: dom.Node, offset: Int) =
    if (offset < node.textContent.size) range.setEnd(node, offset)
    else range.setEndAfter(node)

  // TODO: what a mess
  def makeRange(element: dom.HTMLElement, _start: Int, _end: Int): dom.Range = {
    // TODO: is it needed?
    val start = Math.max(_start, 0)
    val end = Math.max(_end, start)

    val range = dom.document.createRange()

    // @tailrec
    def traverse(queue: List[dom.Node], position: Int, current: Int): Unit =
      queue match {
        case node :: rest =>
          if (node.isTextNode) {
            val length = node.textContent.size
            if ((current + length) >= position) {
              val offset = position - current
              if (position == start) {
                setStart(range, node, offset)
                if (end != start) {
                  traverse(queue, end, current)
                  return
                } else {
                  return
                }
              } else {
                setEnd(range, node, offset)
                return
              }
            }

            val nextCurrent = current + node.textContent.size
            val nextSiblingOpt = Option(node.nextSibling)
            val firstChildOpt = Option(node.firstChild)
            val nextQueue = List(nextSiblingOpt, firstChildOpt).flatten ++ rest
            traverse(nextQueue, position, nextCurrent)
          } else if (node.isBrNode) {
            if ((current + 1) >= position) {
              if (position == start) {
                setStart(range, node, 0)
                if (end != start) {
                  traverse(queue, end, current)
                } else {
                  return
                }
              } else {
                setEnd(range, node, 0)
                return
              }
            }

            val nextCurrent = current + 1
            val nextSiblingOpt = Option(node.nextSibling)
            val firstChildOpt = Option(node.firstChild)
            val nextQueue = List(nextSiblingOpt, firstChildOpt).flatten ++ rest
            traverse(nextQueue, position, nextCurrent)
          } else {
            val nextSiblingOpt = Option(node.nextSibling)
            val firstChildOpt = Option(node.firstChild)
            val nextQueue = List(nextSiblingOpt, firstChildOpt).flatten ++ rest
            traverse(nextQueue, position, current)
          }
        case Nil =>
          setStart(range, element, 0)
          setEnd(range, element, 0)
      }

    val list = List(Option(element.firstChild)).flatten
    traverse(list, start, 0)
    range
  }

  def makeRange(element: dom.HTMLElement, pos: Int): dom.Range =
    makeRange(element, pos, pos)
}
