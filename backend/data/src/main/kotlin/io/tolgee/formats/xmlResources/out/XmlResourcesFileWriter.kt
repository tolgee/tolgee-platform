package io.tolgee.formats.xmlResources.out

import io.tolgee.formats.ExportFormat
import io.tolgee.formats.xmlResources.PluralUnit
import io.tolgee.formats.xmlResources.StringArrayUnit
import io.tolgee.formats.xmlResources.StringUnit
import io.tolgee.formats.xmlResources.XmlResourcesNode
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.formats.xmlResources.XmlResourcesStringsModel
import io.tolgee.util.attr
import io.tolgee.util.buildDom
import io.tolgee.util.comment
import io.tolgee.util.element
import org.w3c.dom.Element
import java.io.InputStream

class XmlResourcesFileWriter(
  private val model: XmlResourcesStringsModel,
  private val exportFormat: ExportFormat,
) {
  fun produceFiles(): InputStream {
    return buildDom {
      element("resources") {
        if (isAndroid) {
          attr("xmlns:xliff", "urn:oasis:names:tc:xliff:document:1.2")
        }
        model.items.forEach { this.addToElement(it) }
      }
    }.write().toByteArray().inputStream()
  }

  private fun Element.addToElement(unit: Map.Entry<String, XmlResourcesNode>) {
    when (val node = unit.value) {
      is StringUnit -> {
        optionalComment(node.comment)
        element("string") {
          attr("name", unit.key)
          appendXmlIfEnabledOrText((unit.value as StringUnit).value)
        }
      }

      is StringArrayUnit -> {
        element("string-array") {
          attr("name", unit.key)
          node.items.sortedBy { it.index }.forEach {
            optionalComment(it.comment)
            element("item") {
              appendXmlIfEnabledOrText(it.value)
            }
          }
        }
      }

      is PluralUnit -> {
        optionalComment(node.comment)
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

  private fun Element.appendXmlIfEnabledOrText(value: XmlResourcesStringValue?) {
    if (value == null) {
      return
    }
    val contentToAppend =
      TextToXmlResourcesConvertor(this.ownerDocument, value, exportFormat)
        .convert()
    if (contentToAppend.text != null) {
      this.textContent = contentToAppend.text
    }

    contentToAppend.children?.forEach {
      val imported = this.ownerDocument.importNode(it, true)
      this.appendChild(imported)
    }
  }

  private fun Element.optionalComment(comment: String?) {
    comment?.takeIf { it.isNotBlank() }?.let { comment(" $it ") }
  }

  val isAndroid
    get() = exportFormat == ExportFormat.ANDROID_XML
}
