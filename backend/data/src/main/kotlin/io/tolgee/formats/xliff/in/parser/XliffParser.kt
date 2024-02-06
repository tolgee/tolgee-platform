package io.tolgee.formats.xliff.`in`.parser

import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
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

  private val result = XliffModel()
  private var currentFile: XliffFile? = null
  private var currentTransUnit: XliffTransUnit? = null

  fun parse(): XliffModel {
    var currentTextValue: String? = null
    parseVersion()
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
                val file = XliffFile()
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
                val transUnit = XliffTransUnit()
                currentTransUnit = transUnit
                currentFile!!.transUnits.add(transUnit)
                transUnit.id = startElement.getAttributeByName(QName(null, "id"))?.value
                transUnit.translate = startElement.getAttributeByName(QName(null, "translate"))?.value
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

  private fun parseVersion() {
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      if (event.isStartElement &&
        (event as? StartElement)?.name?.localPart?.lowercase(Locale.getDefault()) == "xliff"
      ) {
        val versionAttr = event.getAttributeByName(QName(null, "version"))
        if (versionAttr != null) {
          result.version = versionAttr.value
          return
        }
      }
    }
  }

  private val isAnyToContentSaveOpen
    get() = openElements.any { CONTENT_SAVE_ELEMENTS.contains(it) }

  companion object {
    private val CONTENT_SAVE_ELEMENTS = listOf("source", "target", "note")
  }
}
