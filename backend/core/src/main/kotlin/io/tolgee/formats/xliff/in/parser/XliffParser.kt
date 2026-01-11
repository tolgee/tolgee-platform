package io.tolgee.formats.xliff.`in`.parser

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.xliff.model.XliffFile
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.formats.xliff.model.XliffTransUnit
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.events.StartElement

class XliffParser(
  private val xmlEventReader: XMLEventReader,
) {
  private var xw: XMLEventWriter? = null
  private val openElements = mutableListOf("xliff")
  private val currentOpenElement: String?
    get() = openElements.lastOrNull()

  private val result = XliffModel()
  private var currentFile: XliffFile? = null
  private var currentTransUnit: XliffTransUnit? = null
  private var preservingSpaces = mutableListOf<Boolean?>()
  private var blockParser: XliffXmlValueBlockParser? = null

  fun parse(): XliffModel {
    try {
      return doParse()
    } catch (e: Exception) {
      throw ImportCannotParseFileException("XLIFF", e.message)
    } finally {
      xmlEventReader.close()
    }
  }

  private fun doParse(): XliffModel {
    parseVersion()
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      when {
        event.isStartElement -> {
          if (!isAnyToContentSaveOpen) {
            blockParser = XliffXmlValueBlockParser()
          }
          (event as? StartElement)?.let { startElement ->
            preservingSpaces.add(getCurrentElementPreserveSpaces(startElement))
            openElements.add(startElement.name.localPart.lowercase(Locale.getDefault()))
            when (currentOpenElement) {
              "file" -> {
                val file = XliffFile()
                currentFile = file
                result.files.add(file)
                file.original =
                  startElement
                    .getAttributeByName(QName(null, "original"))
                    ?.value
                file.sourceLanguage =
                  startElement
                    .getAttributeByName(QName(null, "source-language"))
                    ?.value
                file.targetLanguage =
                  startElement
                    .getAttributeByName(QName(null, "target-language"))
                    ?.value
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

        event.isEndElement -> {
          when (currentOpenElement) {
            "file" -> {
              currentFile = null
            }

            "trans-unit" -> {
              currentTransUnit = null
            }

            "source" -> {
              currentTransUnit?.let {
                it.source = getCurrentSwString()
              }
            }

            "target" -> {
              currentTransUnit?.let {
                it.target = getCurrentSwString()
              }
            }

            "note" -> {
              currentTransUnit?.let {
                it.note = getCurrentSwString()
              }
            }
          }
          openElements.removeLast()
          preservingSpaces.removeLast()
        }
      }

      if (isAnyToContentSaveOpen) {
        val startName = (event as? StartElement)?.name?.localPart?.lowercase(Locale.getDefault())
        if (!CONTENT_SAVE_ELEMENTS.contains(startName)) {
          blockParser?.onXmlEvent(event)
        }
      }
    }
    return result
  }

  private fun getCurrentElementPreserveSpaces(startElement: StartElement): Boolean? {
    val value = startElement.getAttributeByName(QName(XMLConstants.XML_NS_URI, "space"))?.value
    return when (value) {
      "preserve" -> true
      "default" -> false
      else -> null
    }
  }

  private fun getCurrentSwString(): String {
    val result = blockParser?.result ?: ""
    val preserveNamespace = preservingSpaces.lastOrNull { it != null } ?: false
    if (!preserveNamespace) {
      return result.trim()
    }
    return result
  }

  private fun parseVersion() {
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      if (event.isStartElement &&
        (event as? StartElement)?.name?.localPart?.lowercase(Locale.getDefault()) == "xliff"
      ) {
        preservingSpaces.add(getCurrentElementPreserveSpaces(event))
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
