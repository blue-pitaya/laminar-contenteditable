package dev.bluepitaya.laminarcontenteditable

import org.scalajs.dom

object MutationObserverHelper {
  private val observerConfig = new dom.MutationObserverInit {
    characterData = true;
    characterDataOldValue = true;
    childList = true;
    subtree = true;
  }

  implicit class RichMutationObserver(mo: dom.MutationObserver) {
    def observeElement(el: dom.HTMLElement): Unit = {
      mo.observe(el, observerConfig)
    }
  }
}
