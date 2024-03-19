import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.AndroidStringsXmlModel
import io.tolgee.formats.android.PluralUnit
import io.tolgee.formats.android.StringArrayItem
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.formats.android.`in`.AndroidXmlValueBlockParser
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.StartElement

class AndroidStringsXmlParser(
  private val xmlEventReader: XMLEventReader,
) {
  private val result = AndroidStringsXmlModel()
  private var currentStringEntry: StringUnit? = null
  private var currentArrayEntry: StringArrayUnit? = null
  private var currentPluralEntry: PluralUnit? = null
  private var currentPluralQuantity: String? = null
  private var blockParser: AndroidXmlValueBlockParser? = null
  private var isArrayItemOpen = false

  fun parse(): AndroidStringsXmlModel {
    while (xmlEventReader.hasNext()) {
      val event = xmlEventReader.nextEvent()
      val wasAnyToContentSaveOpenBefore = isAnyToContentSaveOpen
      when {
        event.isStartElement -> {
          if (!isAnyToContentSaveOpen) {
            blockParser = AndroidXmlValueBlockParser()
          }
          val startElement = event as StartElement
          when (startElement.name.localPart.lowercase()) {
            "string" -> {
              val stringEntry = StringUnit()
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
                    .getAttributeByName(QName(null, "quantity"))?.value
              } else if (currentArrayEntry != null) {
                isArrayItemOpen = true
              }
            }

            "plurals" -> {
              val pluralEntry = PluralUnit()
              getKeyName(startElement)?.let { keyName ->
                currentPluralEntry = pluralEntry
                result.items[keyName] = pluralEntry
              }
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
                val index = currentArrayEntry?.items?.size ?: 0
                currentArrayEntry?.items?.add(StringArrayItem(getCurrentTextOrXml(), index))
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
          blockParser?.onXmlEvent(event)
        }
      }
    }

    return result
  }

  private fun getKeyName(startElement: StartElement) =
    startElement.getAttributeByName(
      QName(null, "name"),
    )?.value

  private fun getCurrentTextOrXml(): AndroidStringValue {
    return blockParser?.result ?: AndroidStringValue("", false)
  }

  private val isAnyToContentSaveOpen: Boolean
    get() {
      return currentStringEntry != null || isArrayItemOpen || currentPluralQuantity != null
    }
}
