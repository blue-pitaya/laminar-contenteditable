package dev.bluepitaya.laminarcontenteditable

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

  def toTextContent(element: dom.HTMLElement): String = {
    case class QueueItem(node: dom.Node, isFirstChild: Boolean)

    def getText(q: QueueItem): String = {
      q.node match {
        case n if n.isTextNode                   => n.textContent
        case n if n.isDivNode && !q.isFirstChild => "\n"
        case _                                   => ""
      }
    }

    def getChildren(queueItem: QueueItem): Seq[QueueItem] = {
      val items = queueItem.node.childNodes.toSeq.map(n => QueueItem(n, false))
      items match {
        case head :: next => head.copy(isFirstChild = true) :: next
        case Nil          => Nil
      }
    }

    def traverse(queue: Seq[QueueItem], content: String): String = queue match {
      case head :: rest =>
        val nextContent = content + getText(head)
        val children = getChildren(head)

        val nextQueue = children ++ rest
        traverse(nextQueue, nextContent)
      case Nil => content
    }

    traverse(Seq(QueueItem(element, true)), "")
  }

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
}
