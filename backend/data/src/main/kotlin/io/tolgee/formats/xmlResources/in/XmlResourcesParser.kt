package io.tolgee.formats.xmlResources.`in`

import io.tolgee.formats.xmlResources.PluralUnit
import io.tolgee.formats.xmlResources.StringArrayItem
import io.tolgee.formats.xmlResources.StringArrayUnit
import io.tolgee.formats.xmlResources.StringUnit
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.formats.xmlResources.XmlResourcesStringsModel
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.events.Comment
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class XmlResourcesParser(
  private val xmlEventReader: XMLEventReader,
  private val stringUnescaper: StringUnescaper,
  private val supportedTags: Set<String>,
) {
  private val result = XmlResourcesStringsModel()
  private var currentComment: String? = null
  private var currentStringEntry: StringUnit? = null
  private var currentArrayEntry: StringArrayUnit? = null
  private var currentPluralEntry: PluralUnit? = null
  private var currentPluralQuantity: String? = null
  private var blockParser: XmlResourcesValueBlockParser? = null
  private var isArrayItemOpen = false
  private var arrayItemComment: String? = null

  fun parse(): XmlResourcesStringsModel {
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      val wasAnyToContentSaveOpenBefore = isAnyToContentSaveOpen
      when {
        event.isComment -> {
          currentComment = event.asComment().text
        }

        event.isStartElement -> {
          if (!isAnyToContentSaveOpen) {
            blockParser =
              XmlResourcesValueBlockParser(
                stringUnescaper,
                supportedTags,
              )
          }
          val startElement = event as StartElement
          when (startElement.name.localPart.lowercase()) {
            "string" -> {
              val stringEntry = StringUnit()
              stringEntry.comment = currentComment
              getKeyName(startElement)?.let { keyName ->
                currentStringEntry = stringEntry
                result.items[keyName] = stringEntry
              }
            }

            "string-array" -> {
              val arrayEntry = StringArrayUnit()
              getKeyName(startElement)?.let { keyName ->
                currentArrayEntry = arrayEntry
                result.items[keyName] = arrayEntry
              }
            }

            "item" -> {
              if (currentPluralEntry != null) {
                currentPluralQuantity =
                  startElement
                    .getAttributeByName(QName(null, "quantity"))
                    ?.value
              } else if (currentArrayEntry != null) {
                isArrayItemOpen = true
                arrayItemComment = currentComment
              }
            }

            "plurals" -> {
              val pluralEntry = PluralUnit()
              pluralEntry.comment = currentComment
              getKeyName(startElement)?.let { keyName ->
                currentPluralEntry = pluralEntry
                result.items[keyName] = pluralEntry
              }
            }
          }
          currentComment = null
        }

        event.isEndElement -> {
          currentComment = null
          when (
            event
              .asEndElement()
              .name.localPart
              .lowercase()
          ) {
            "string" -> {
              currentStringEntry?.value = getCurrentTextOrXml()
              currentStringEntry = null
            }

            "item" -> {
              if (currentPluralEntry != null) {
                if (currentPluralQuantity != null) {
                  currentPluralEntry!!.items[currentPluralQuantity!!] = getCurrentTextOrXml()
                  currentPluralQuantity = null
                }
              } else if (isArrayItemOpen) {
                val index = currentArrayEntry?.items?.size ?: 0
                currentArrayEntry?.items?.add(StringArrayItem(getCurrentTextOrXml(), index, arrayItemComment))
                isArrayItemOpen = false
                arrayItemComment = null
              }
            }

            "plurals" -> {
              currentPluralEntry = null
            }

            "string-array" -> {
              currentArrayEntry = null
            }
          }
        }
      }

      if (isAnyToContentSaveOpen) {
        currentComment = null
        if (wasAnyToContentSaveOpenBefore) {
          blockParser?.onXmlEvent(event)
        }
      }
    }

    return result
  }

  private fun getKeyName(startElement: StartElement) =
    startElement
      .getAttributeByName(
        QName(null, "name"),
      )?.value

  private fun getCurrentTextOrXml(): XmlResourcesStringValue {
    return blockParser?.result ?: XmlResourcesStringValue("", false)
  }

  private val isAnyToContentSaveOpen: Boolean
    get() {
      return currentStringEntry != null || isArrayItemOpen || currentPluralQuantity != null
    }

  private val XMLEvent.isComment: Boolean
    get() = this.eventType == XMLStreamConstants.COMMENT

  private fun XMLEvent.asComment(): Comment = this as Comment
}
