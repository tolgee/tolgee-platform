package io.tolgee.formats.android.out

import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.AndroidStringsXmlModel
import io.tolgee.formats.android.AndroidXmlNode
import io.tolgee.formats.android.PluralUnit
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.util.attr
import io.tolgee.util.buildDom
import io.tolgee.util.element
import org.w3c.dom.Element
import java.io.InputStream

class AndroidStringsXmlFileWriter(private val model: AndroidStringsXmlModel) {
  fun produceFiles(): InputStream {
    return buildDom {
      element("resources") {
        attr("xmlns:xliff", "urn:oasis:names:tc:xliff:document:1.2")
        model.items.forEach { this.addToElement(it) }
      }
    }.write().toByteArray().inputStream()
  }

  private fun Element.addToElement(unit: Map.Entry<String, AndroidXmlNode>) {
    when (val node = unit.value) {
      is StringUnit -> {
        element("string") {
          attr("name", unit.key)
          appendXmlIfEnabledOrText((unit.value as StringUnit).value)
        }
      }

      is StringArrayUnit -> {
        element("string-array") {
          attr("name", unit.key)
          node.items.sortedBy { it.index }.forEach {
            element("item") {
              appendXmlIfEnabledOrText(it.value)
            }
          }
        }
      }

      is PluralUnit -> {
        element("plurals") {
          attr("name", unit.key)
          node.items.forEach {
            element("item") {
              attr("quantity", it.key)
              appendXmlIfEnabledOrText(it.value)
            }
          }
        }
      }
    }
  }

  private fun Element.appendXmlIfEnabledOrText(value: AndroidStringValue?) {
    if (value == null) {
      return
    }
    val contentToAppend =
      TextToAndroidXmlConvertor(this.ownerDocument, value)
        .convert()
    if (contentToAppend.text != null) {
      this.textContent = contentToAppend.text
    }

    if (contentToAppend.children != null) {
      contentToAppend.children.forEach {
        val imported = this.ownerDocument.importNode(it, true)
        this.appendChild(imported)
      }
    }
  }
}
