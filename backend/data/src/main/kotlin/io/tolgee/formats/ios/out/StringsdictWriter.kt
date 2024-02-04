package io.tolgee.formats.ios.out

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream

class StringsdictWriter {
  private val document: Document = DocumentHelper.createDocument()
  private val root = DocumentHelper.createElement("plist")

  init {
    document.add(root)
    document.addDocType("plist", "-//Apple//DTD PLIST 1.0//EN", "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
    root.addAttribute("version", "1.0")
    root.add(DocumentHelper.createElement("dict"))
  }

  fun addEntry(
    key: String,
    pluralForms: Map<String, String>,
  ) {
    val dictElement = root.element("dict")

    val keyElement = DocumentHelper.createElement("key")
    keyElement.text = key
    dictElement.add(keyElement)

    val dictValueElement = DocumentHelper.createElement("dict")
    dictElement.add(dictValueElement)

    // add other necessary elements as needed
    val keyLocalizedFormatElement = DocumentHelper.createElement("key")
    keyLocalizedFormatElement.text = "NSStringLocalizedFormatKey"
    dictValueElement.add(keyLocalizedFormatElement)

    val stringLocalizedFormatElement = DocumentHelper.createElement("string")
    stringLocalizedFormatElement.text = "%#\${#@format@}"
    dictValueElement.add(stringLocalizedFormatElement)

    val keyFormatElement = DocumentHelper.createElement("key")
    keyFormatElement.text = "format"
    dictValueElement.add(keyFormatElement)

    val dictFormatElement = DocumentHelper.createElement("dict")
    dictValueElement.add(dictFormatElement)

    pluralForms.forEach { (formKey, translation) ->

      val keyQuantityElement = DocumentHelper.createElement("key")
      keyQuantityElement.text = formKey
      dictFormatElement.add(keyQuantityElement)

      val stringQuantityElement = DocumentHelper.createElement("string")
      stringQuantityElement.text = translation
      dictFormatElement.add(stringQuantityElement)
    }
  }

  val result: InputStream
    get() {
      val format = OutputFormat.createPrettyPrint()
      val outputStream = ByteArrayOutputStream()
      val writer = XMLWriter(outputStream, format)
      writer.write(document)
      return outputStream.toByteArray().inputStream()
    }
}
