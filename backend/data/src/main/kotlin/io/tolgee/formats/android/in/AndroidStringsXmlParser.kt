import io.tolgee.formats.android.PluralsUnit
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.formats.android.StringsModel
import java.io.StringWriter
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.StartElement

class AndroidStringsXmlParser(
  private val xmlEventReader: XMLEventReader,
) {
  private val result = StringsModel()
  private var currentStringEntry: StringUnit? = null
  private var currentArrayEntry: StringArrayUnit? = null
  private var currentPluralEntry: PluralsUnit? = null
  private var currentPluralQuantity: String? = null
  private var sw = StringWriter()
  private var xw: XMLEventWriter? = null
  private val of: XMLOutputFactory = XMLOutputFactory.newDefaultFactory()
  private var isArrayItemOpen = false

  fun parse(): StringsModel {
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      val wasAnyToContentSaveOpenBefore = isAnyToContentSaveOpen
      when {
        event.isStartElement -> {
          if (!isAnyToContentSaveOpen) {
            sw = StringWriter()
            xw = of.createXMLEventWriter(sw)
          }
          val startElement = event as StartElement
          when (startElement.name.localPart.lowercase()) {
            "string" -> {
              val stringEntry = StringUnit()
              currentStringEntry = stringEntry
              result.strings.add(stringEntry)
              stringEntry.name = startElement.getAttributeByName(QName(null, "name"))?.value
            }

            "string-array" -> {
              val arrayEntry = StringArrayUnit()
              currentArrayEntry = arrayEntry
              result.stringArrays.add(arrayEntry)
              arrayEntry.name = startElement.getAttributeByName(QName(null, "name"))?.value
            }

            "item" -> {
              if (currentPluralEntry != null) {
                currentPluralQuantity = startElement.getAttributeByName(QName(null, "quantity"))?.value
              } else if (currentArrayEntry != null) {
                isArrayItemOpen = true
              }
            }

            "plurals" -> {
              val pluralEntry = PluralsUnit()
              currentPluralEntry = pluralEntry
              result.plurals.add(pluralEntry)
              pluralEntry.name = startElement.getAttributeByName(QName(null, "name"))?.value
            }
          }
        }

        event.isEndElement -> {
          when (event.asEndElement().name.localPart.lowercase()) {
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
                currentArrayEntry?.items?.add(getCurrentTextOrXml())
                isArrayItemOpen = false
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
        if (wasAnyToContentSaveOpenBefore) {
          xw?.add(event)
        }
      } else {
        xw?.close()
      }
    }

    return result
  }

  private fun getCurrentTextOrXml(): String {
    return sw.toString()
      // android doesn't seem to support xml:space="preserve"
      .trim()
  }

  private val isAnyToContentSaveOpen: Boolean
    get() {
      return currentStringEntry != null || isArrayItemOpen || currentPluralQuantity != null
    }
}
