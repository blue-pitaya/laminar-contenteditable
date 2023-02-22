package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.annotation.tailrec

object DomNodeExtensions {
  implicit class RichNode(node: dom.Node) {
    def isTextNode: Boolean = node.nodeType == dom.Node.TEXT_NODE
    def isBrNode: Boolean = node.nodeType == dom.Node.ELEMENT_NODE &&
      node.nodeName == "BR"
    def isDivNode: Boolean = node.nodeType == dom.Node.ELEMENT_NODE &&
      node.nodeName == "DIV"
  }
}

object Parser {
  import DomNodeExtensions.RichNode

  private def nodeToString(node: dom.Node): String =
    if (node.isTextNode) node.textContent
    else if (node.isDivNode) "\n"
    else ""

  private def updateQueue(
      queue: Seq[dom.Node],
      currentNode: dom.Node
  ): Seq[dom.Node] = {
    val childOpt = Option(currentNode.firstChild)
    val siblingOpt = Option(currentNode.nextSibling)

    Seq(childOpt).flatten ++ Seq(siblingOpt).flatten ++ queue
  }

  // TODO: nested divs are parsed badly
  def toTextContent(element: dom.HTMLElement): String = {
    @tailrec
    def traverseNodes(queue: Seq[dom.Node], content: String): String =
      queue match {
        case node :: rest =>
          val nextContent = content + nodeToString(node)
          val nextQueue = updateQueue(rest, node)
          traverseNodes(nextQueue, nextContent)
        case Nil => content
      }

    traverseNodes(Seq(element), "")
  }

  // TODO: windows endings?
  def toHtmlContent(content: String): String = content
    .split("\n", -1)
    .zipWithIndex
    .map { case (line, idx) =>
      val lineContent =
        if (line.isEmpty()) "<br/>"
        else line

      if (idx == 0) lineContent
      else s"<div>${lineContent}</div>"
    }
    .mkString

  def toNodes(content: String): Seq[Node] = {
    println("czaje sei")
    content
      .split("\n", -1)
      .zipWithIndex
      .map { case (line, idx) =>
        val lineContent: Node =
          if (line.isEmpty()) br()
          else line

        if (idx == 0) lineContent
        else div(lineContent)
      }
  }
}
