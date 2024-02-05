package io.tolgee.formats.xliff.`in`.parser

import java.io.StringWriter
import java.util.*
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.StartElement

class XliffParser(
  private val xmlEventReader: XMLEventReader,
) {
  private var sw = StringWriter()
  private val of: XMLOutputFactory = XMLOutputFactory.newDefaultFactory()
  private var xw: XMLEventWriter? = null
  private val openElements = mutableListOf("xliff")
  private val currentOpenElement: String?
    get() = openElements.lastOrNull()

  private val result = XliffParserResult()
  private var currentFile: XliffParserResultFile? = null
  private var currentTransUnit: XliffParserResultTransUnit? = null

  fun parse(): XliffParserResult {
    var currentTextValue: String? = null

    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      when {
        event.isStartElement -> {
          if (!isAnyToContentSaveOpen) {
            sw = StringWriter()
            xw = of.createXMLEventWriter(sw)
          }
          (event as? StartElement)?.let { startElement ->
            openElements.add(startElement.name.localPart.lowercase(Locale.getDefault()))
            when (currentOpenElement) {
              "file" -> {
                val file = XliffParserResultFile()
                currentFile = file
                result.files.add(file)
                file.original =
                  startElement
                    .getAttributeByName(QName(null, "original"))?.value
                file.sourceLanguage =
                  startElement
                    .getAttributeByName(QName(null, "source-language"))?.value
                file.targetLanguage =
                  startElement
                    .getAttributeByName(QName(null, "target-language"))?.value
              }

              "trans-unit" -> {
                if (currentFile == null) {
                  throw IllegalStateException("Unexpected trans-unit element")
                }
                val transUnit = XliffParserResultTransUnit()
                currentTransUnit = transUnit
                currentFile!!.transUnits.add(transUnit)
                transUnit.id = startElement.getAttributeByName(QName(null, "id"))?.value
              }
            }
          }
        }

        event.isCharacters -> {
          if (currentOpenElement != null) {
            when (currentOpenElement!!) {
              in "source", "target", "note" -> {
                currentTextValue = (currentTextValue ?: "") + event.asCharacters().data
              }
            }
          }
        }

        event.isEndElement ->
          if (event.isEndElement) {
            when (currentOpenElement) {
              "file" -> {
                currentFile = null
              }

              "trans-unit" -> {
                currentTransUnit = null
              }

              "source" -> {
                currentTransUnit?.let {
                  it.source = sw.toString()
                }
              }

              "target" -> {
                currentTransUnit?.let {
                  it.target = sw.toString()
                }
              }

              "note" -> {
                currentTransUnit?.let {
                  it.note = sw.toString()
                }
              }
            }
            currentTextValue = null
            openElements.removeLast()
          }
      }
      if (isAnyToContentSaveOpen) {
        val startName = (event as? StartElement)?.name?.localPart?.lowercase(Locale.getDefault())
        if (!CONTENT_SAVE_ELEMENTS.contains(startName)) {
          xw?.add(event)
        }
      } else {
        xw?.close()
      }
    }
    return result
  }

  private val isAnyToContentSaveOpen
    get() = openElements.any { CONTENT_SAVE_ELEMENTS.contains(it) }

  companion object {
    private val CONTENT_SAVE_ELEMENTS = listOf("source", "target", "note")
  }
}
