package xyz.bluepitaya.laminarcontenteditable

import com.raquo.laminar.api.L._
import org.scalajs.dom

import MutationObserverHelper._

case class State(text: String, caretPosition: CaretPosition) {
  def updateText(v: String) = copy(text = v)
  def updateCaret(v: CaretPosition) = copy(caretPosition = v)
}

object LamEditor {
  def component(parseText: String => Seq[Node] = Parser.toNodes) = {
    val mutObs = Var[Option[dom.MutationObserver]](None)
    val lastHtml = Var("")
    val state = Var(State("", CaretPosition(0, 0)))
    val runContentChangeCallback = Var(false)

    def disableMutObs() = mutObs.now().foreach(_.disconnect())
    def enableMutObs(el: dom.Element) = mutObs
      .now()
      .foreach(_.observeElement(el))

    def contentSignal(el: dom.HTMLElement): Signal[Seq[Node]] = state
      .signal
      .map { s =>
        println(s"content signal ${s.text}")
        disableMutObs()
        el.innerHTML = lastHtml.now()
        runContentChangeCallback.set(true)
        parseText(s.text)
      }

    def flushChanges(element: dom.HTMLElement): Unit = {
      println("change detected!")
      val textContent = Parser.toTextContent(element)
      val caretPosition = CaretOps.getPosition(element)
      state.update(s => s.updateText(textContent).updateCaret(caretPosition))
    }

    div(
      pre(
        contentEditable(true),
        whiteSpace.preWrap,
        width("100%"),
        height("100%"),
        margin("0"),
        outline("none"),
        inContext { el =>
          Seq(
            children <-- contentSignal(el.ref),
            runContentChangeCallback -->
              Observer[Boolean] { v =>
                lastHtml.set(el.ref.innerHTML)
                println(s"reconnect mut obs $v")
                if (v) {
                  CaretOps.setCaretPosition(el.ref, state.now().caretPosition)
                  enableMutObs(el.ref)
                  runContentChangeCallback.set(false)
                } else ()
              }
          )
        },
        // TODO: possible leak of mutationObserver?
        onMountCallback { ctx =>
          val element = ctx.thisNode.ref
          val mutationObserver = new dom.MutationObserver((_, mutObs) => {
            flushChanges(element)
          })
          mutObs.set(Some(mutationObserver))
          enableMutObs(element)
        }
      ),
      padding("10px"),
      border("1px solid black"),
      width("500px"),
      height("500px"),
      overflowY.auto
    )
  }

}
