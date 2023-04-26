package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom
import scala.util.Try

case class CaretPosition(pos: Int, extent: Int)

object CaretPosition {
  def default = CaretPosition(0, 0)
}

object CaretOps {
  import DomNodeExtensions.RichNode

  def setCaretPosition(
      caretPosition: CaretPosition,
      element: dom.HTMLElement
  ): Unit = {
    val range = CaretOps.makeRange(element, caretPosition)
    CaretOps.setCurrentRange(range)
  }

  private def getCurrentRange: Try[dom.Range] =
    Try(dom.window.getSelection().getRangeAt(0))

  private def setCurrentRange(range: dom.Range): Unit = {
    val selection = dom.window.getSelection()
    selection.removeAllRanges() // empty()
    selection.addRange(range)
  }

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

  def getPosition(element: dom.HTMLElement): Option[CaretPosition] = {
    val range = getCurrentRange
    range
      .map { range =>
        def traverse(
            queue: Seq[dom.Node],
            currentOffset: Int,
            rangeContainer: dom.Node,
            rangeOffset: Int
        ): Int = queue match {
          case node :: rest =>
            if (node == rangeContainer) {
              val additionalOffset =
                if (node.isDivNode) 1
                else 0
              currentOffset + rangeOffset + additionalOffset
            } else {
              val nodeContentSize = getContentSize(node)
              traverse(
                updateQueue(rest, node),
                currentOffset + nodeContentSize,
                rangeContainer,
                rangeOffset
              )
            }
          case Nil => currentOffset
        }

        val position =
          traverse(Seq(element), 0, range.startContainer, range.startOffset)
        val extent =
          traverse(Seq(element), 0, range.endContainer, range.endOffset) -
            position
        Some(CaretPosition(position, extent))
      }
      .getOrElse(None)
  }

  // TODO: make extent also!
  private def makeRange(
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
