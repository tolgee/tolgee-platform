package io.tolgee.formats.resx.out

import io.tolgee.formats.resx.ResxEntry
import io.tolgee.util.attr
import io.tolgee.util.buildDom
import io.tolgee.util.element
import org.w3c.dom.Element
import java.io.InputStream

class ResxWriter(
  private val model: List<ResxEntry>,
) {
  fun produceFiles(): InputStream {
    return buildDom {
      element("root") {
        addHeaders()
        model.forEach { addToElement(it) }
      }
    }.write().toByteArray().inputStream()
  }

  private fun Element.addHeaders() {
    element("resheader") {
      attr("name", "resmimetype")
      element("value") {
        textContent = "text/microsoft-resx"
      }
    }
    element("resheader") {
      attr("name", "version")
      element("value") {
        textContent = "2.0"
      }
    }
    element("resheader") {
      attr("name", "reader")
      element("value") {
        textContent = "System.Resources.ResXResourceReader, System.Windows.Forms, Version=4.0.0.0, Culture=neutral"
      }
    }
    element("resheader") {
      attr("name", "writer")
      element("value") {
        textContent = "System.Resources.ResXResourceWriter, System.Windows.Forms, Version=4.0.0.0, Culture=neutral"
      }
    }
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
