package io.tolgee.formats.resx.`in`

import io.tolgee.formats.resx.ResxEntry
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.Characters
import javax.xml.stream.events.StartElement

class ResxParser(
  private val xmlEventReader: XMLEventReader,
) {
  fun parse(): Sequence<ResxEntry> =
    sequence {
      var currentEntry: ResxEntry? = null
      var currentData: String? = null
      var currentComment: String? = null

      fun isAnyToContentSaveOpen(): Boolean = currentData != null || currentComment != null

      while (xmlEventReader.hasNext()) {
        val event = xmlEventReader.nextEvent()
        val wasAnyToContentSaveOpenBefore = isAnyToContentSaveOpen()
        when {
          event.isStartElement -> {
            val element = event.asStartElement()
            val name = element.name.localPart.lowercase()
            if (isAnyToContentSaveOpen()) {
              throw IllegalStateException("Unexpected start of xml element: $name")
            }
            when (name) {
              "data" -> {
                element.getKeyName()?.let { keyName ->
                  val entry = ResxEntry(keyName)
                  currentEntry = entry
                }
              }

              "value" -> {
                if (currentEntry != null) {
                  currentData = ""
                  currentComment = null
                }
              }

              "comment" -> {
                if (currentEntry != null) {
                  currentComment = ""
                  currentData = null
                }
              }
            }
          }

          event.isEndElement -> {
            val element = event.asEndElement()
            val name = element.name.localPart.lowercase()
            when (name) {
              "data" -> {
                currentEntry?.let { yield(it) }
                currentEntry = null
                currentData = null
                currentComment = null
              }

              "value" -> {
                currentData?.let {
                  currentEntry = currentEntry?.copy(data = it)
                }
                currentData = null
              }

              "comment" -> {
                currentComment?.let {
                  currentEntry = currentEntry?.copy(comment = it)
                }
                currentComment = null
              }
            }
          }
        }

        if (isAnyToContentSaveOpen() && wasAnyToContentSaveOpenBefore && event is Characters) {
          val text = event.asCharacters().data
          when {
            currentData != null -> {
              currentData += text
            }
            currentComment != null -> {
              currentComment += text
            }
          }
        }
      }
    }

  private fun StartElement.getKeyName() = getAttributeByName(QName(null, "name"))?.value
}
