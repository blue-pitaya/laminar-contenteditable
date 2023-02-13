package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

case class CaretPosition(pos: Int, extent: Int)

object CaretOps {
  import DomNodeExtensions.RichNode

  def getCurrentRange: dom.Range = dom.window.getSelection().getRangeAt(0)

  def setCurrentRange(range: dom.Range): Unit = {
    val selection = dom.window.getSelection()
    selection.removeAllRanges() // empty()
    selection.addRange(range)
  }

  // TODO: dup in Parser code
  private def updateQueue(
      queue: Seq[dom.Node],
      currentNode: dom.Node
  ): Seq[dom.Node] = {
    val childOpt = Option(currentNode.firstChild)
    val siblingOpt = Option(currentNode.nextSibling)

    Seq(childOpt).flatten ++ Seq(siblingOpt).flatten ++ queue
  }

  private def getContentSize(node: dom.Node): Int =
    if (node.isTextNode) node.textContent.size
    // Div has no content, but generates newline
    else if (node.isDivNode) 1
    else 0

  // TODO: extent
  def getPosition(element: dom.HTMLElement): CaretPosition = {
    val range = getCurrentRange
    val extent =
      if (!range.collapsed) range.toString().size
      else 0

    def traverse(queue: Seq[dom.Node], currentOffset: Int): Int = queue match {
      case node :: rest =>
        if (node == range.startContainer) {
          val additionalOffset =
            if (node.isDivNode) 1
            else 0
          currentOffset + range.startOffset + additionalOffset
        } else {
          val nodeContentSize = getContentSize(node)
          traverse(updateQueue(rest, node), currentOffset + nodeContentSize)
        }
      case Nil => currentOffset
    }

    val position = traverse(Seq(element), 0)
    CaretPosition(position, extent)

  }

  // TODO: make extent also!
  def makeRange(
      element: dom.HTMLElement,
      caretPos: CaretPosition
  ): dom.Range = {
    val pos = caretPos.pos

    def traverse(
        queue: Seq[dom.Node],
        range: dom.Range,
        currentOffset: Int
    ): dom.Range = queue match {
      case node :: rest =>
        val nodeContentSize = getContentSize(node)
        if ((currentOffset + nodeContentSize) >= pos) {
          val offset =
            if (node.isDivNode) 0
            else pos - currentOffset
          range.setStart(node, offset)
          // setEnd(range, node, offset)
          range
        } else traverse(
          updateQueue(rest, node),
          range,
          currentOffset + nodeContentSize
        )
      case Nil => range
    }

    traverse(Seq(element), dom.document.createRange(), 0)
  }
}
