package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

import scala.collection.immutable
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

object ContentEditable {
  import Utils._
  import DomNodeExtensions.RichNode

  def log(x: Any) = dom.console.log(x)

  case class Position(position: Int, extent: Int, content: String, line: Int)

  def getCurrentRange: dom.Range = dom.window.getSelection().getRangeAt(0)

  def setCurrentRange(range: dom.Range): Unit = {
    val selection = dom.window.getSelection()
    selection.removeAllRanges() // empty()
    selection.addRange(range)
  }

  // TODO: keyCode is obsolete, change it to event.code when Laminar is updated
  def isUndoRedoKey(event: dom.KeyboardEvent): Boolean =
    (event.metaKey || event.ctrlKey) && !event.altKey && event.keyCode == 'z'

  def htmlElementToString(element: dom.HTMLElement): String = {
    def getContent(node: dom.Node): String =
      if (node.isTextNode) node.textContent
      else if (node.isBrNode) "\n"
      else ""

    def traverse(queue: List[dom.Node], content: String): String = queue match {
      case node :: rest =>
        val nextContent = content + getContent(node)
        val nextSiblingOpt = Option(node.nextSibling)
        val firstChildOpt = Option(node.firstChild)
        val nextQueue = rest ++ List(nextSiblingOpt, firstChildOpt).flatten
        traverse(nextQueue, nextContent)
      case immutable.Nil => content
    }

    val queue = List(Option(element.firstChild)).flatten
    traverse(queue, "")
  }

  // TODO: is toString on range good?
  def getPosition(element: dom.HTMLElement): Position = {
    val range = getCurrentRange
    val extent =
      if (!range.collapsed) range.toString().size
      else 0
    val untilRange = dom.document.createRange()
    untilRange.setStart(element, 0)
    untilRange.setEnd(range.startContainer, range.startOffset)
    val content = untilRange.toString()
    val position = content.size
    val lines = content.split("\n")
    val line = lines.size - 1
    val realContent = lines(line) // TODO: possible exception!

    Position(position, extent, realContent, line)
  }

  case class State(position: Option[Position]) {
    def addToPosition(n: Int) = copy(position =
      position match {
        case None    => None
        case Some(p) => Some(p.copy(position = p.position + n))
      }
    )
  }
  case class HistoryEvent(position: Position, v: String)

  /** indentation enables and how many */
  case class Options(indentation: Option[Int])

  // def update(
  //    content: String,
  //    element: dom.HTMLElement,
  //    state: Var[State]
  // ): Unit = {
  //  val position = getPosition(element)
  //  val prevContent = htmlElementToString(element)
  //  state.update(s => s.addToPosition(content.size - prevContent.size))
  //  // state.onChange cb
  // }

  def insert(
      append: String,
      deleteOffset: Option[Int],
      element: dom.HTMLElement,
      position: Position
  ): Unit = {
    // If text is selected then remove it
    val range = getCurrentRange
    range.deleteContents()
    range.collapse(false)
    val position = getPosition(element)
    val offset = deleteOffset.getOrElse(0)
    val start = position.position + Math.min(offset, 0)
    val end = position.position + Math.max(offset, 0)
    val range2 = makeRange(element, start, end)
    range2.deleteContents()
    if (!append.isEmpty()) {
      log(append.size)
      range2.insertNode(dom.document.createTextNode(append))
    }

    setCurrentRange(makeRange(element, start + append.size))
  }

  def onKeyDown(
      event: dom.KeyboardEvent,
      state: Var[State],
      element: dom.HTMLElement,
      options: Options
  ): Unit = {
    // Firefox don't support it, so we make it default
    val hasPlainTextSupport = false

    if (event.defaultPrevented || event.target != element) return;

    if (isUndoRedoKey(event)) {
      event.preventDefault()
      // TODO: implement history for input
    }

    if (event.key == "Enter") {
      event.preventDefault()
      // Firefox Quirk: Since plaintext-only is unsupported we must
      // ensure that only newline characters are inserted
      val position = getPosition(element)
      // We also get the current line and preserve indentation for the next
      // line that's created
      val notWhitespaceRegex = """\S""".r
      val index = notWhitespaceRegex
        .findFirstMatchIn(position.content)
        .map(x => x.start)
        .getOrElse(position.content.size)
      val text = "\n" + position.content.slice(0, index)
      // insert(text, None, element, position)
    } else if (
      (!hasPlainTextSupport || options.indentation.isDefined) &&
      event.key == "Backspace"
    ) {
      event.preventDefault()
      val range = getCurrentRange
      if (!range.collapsed) {
        // edit.insert("", 0)
      } else {
        // const position = getPosition(element);
        // const match = blanklineRe.exec(position.content);
        // edit.insert("", match ? -match[1].length : -1);
      }
    } else if (options.indentation.isDefined && event.key == "Tab") {
      event.preventDefault()
      // const position = getPosition(element);
      // const start = position.position - position.content.length;
      // const content = toString(element);
      // const newContent = event.shiftKey
      //  ? content.slice(0, start) +
      //    position.content.replace(indentRe, "") +
      //    content.slice(start + position.content.length)
      //  : content.slice(0, start) +
      //    (opts!.indentation ? " ".repeat(opts!.indentation) : "\t") +
      //    content.slice(start);
      // edit.update(newContent);
    }

    //// Flush changes as a key is held so the app can catch up
    // if (event.repeat) flushChanges();
  }

  def flushChanges(element: dom.HTMLElement, state: Var[State]): Unit = {
    val position = getPosition(element)
    state.update(s => s.copy(position = Some(position)))
  }

  def onKeyUp(
      event: dom.KeyboardEvent,
      element: dom.HTMLElement,
      state: Var[State]
  ): Unit = {
    // TODO: || event.isComposing
    if (event.defaultPrevented) return;
    if (!isUndoRedoKey(event)) {
      // trackState for history
    }
    flushChanges(element, state);

    // Chrome Quirk: The contenteditable may lose focus after the first edit or so
    element.focus();
  }

  def onSelect(
      event: dom.Event,
      state: Var[State],
      element: dom.HTMLElement
  ): Unit = {
    // Chrome Quirk: The contenteditable may lose its selection immediately on first focus
    val nextPosition: Option[Position] =
      if (dom.window.getSelection().rangeCount > 0 && event.target == element)
        Some(getPosition(element))
      else None

    state.update(s => s.copy(position = nextPosition))
  }

  def onPaste(event: dom.ClipboardEvent): Unit = {
    event.preventDefault()
    // trackState(true);
    // edit.insert(event.clipboardData!.getData("text/plain"));
    // trackState(true);
    // flushChanges();
  }

  def events(state: Var[State], element: dom.HTMLElement, options: Options) =
    Seq(
      // TODO: this should be de facto "onselectstart"
      documentEvents.onSelect -->
        Observer[dom.Event] { e =>
          onSelect(e, state, element)
        },
      documentEvents.onKeyDown -->
        Observer[dom.KeyboardEvent] { e =>
          onKeyDown(e, state, element, options)
        },
      documentEvents.onPaste -->
        Observer[dom.ClipboardEvent] { e =>
          onPaste(e)
        },
      documentEvents.onKeyUp -->
        Observer[dom.KeyboardEvent] { e =>
          onKeyUp(e, element, state)
        }
    )
}
