package io.tolgee.formats.resx.out

import io.tolgee.formats.resx.ResxEntry
import io.tolgee.util.attr
import io.tolgee.util.buildDom
import io.tolgee.util.element
import org.w3c.dom.Element
import java.io.InputStream

class ResxWriter(private val model: List<ResxEntry>) {
  fun produceFiles(): InputStream {
    return buildDom {
      element("root") {
        element("reshader") {
          attr("name", "resmimetype")
          element("value") {
            textContent = "text/microsoft-resx"
          }
        }
        element("reshader") {
          attr("name", "version")
          element("value") {
            textContent = "2.0"
          }
        }
        model.forEach { this.addToElement(it) }
      }
    }.write().toByteArray().inputStream()
  }

  private fun Element.addToElement(entry: ResxEntry) {
    element("data") {
      attr("name", entry.key)
      attr("xml:space", "preserve")
      entry.data?.let {
        element("value") {
          textContent = it
        }
      }
      entry.comment?.let {
        element("comment") {
          textContent = it
        }
      }
    }
  }
}
