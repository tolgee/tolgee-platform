package io.tolgee.formats.resx.`in`

import io.tolgee.formats.resx.ResxEntry
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class ResxParser(
  private val xmlEventReader: XMLEventReader,
) {
  var curState: State? = null
  var currentEntry: ResxEntry? = null
  var currentText: String = ""

  fun parse(): Sequence<ResxEntry> =
    sequence {
      while (xmlEventReader.hasNext()) {
        val event = xmlEventReader.nextEvent()
        when {
          event.isStartElement -> handleStartElement(event.asStartElement())
          event.isEndElement -> handleEndElement(event.asEndElement())
        }
        handleText(event)
      }
    }

  fun handleStartElement(element: StartElement) {
    val name = element.name.localPart.lowercase()
    if (isAnyToContentSaveOpen()) {
      throw IllegalStateException("Unexpected start of xml element: $name")
    }
    when (name) {
      "data" -> {
        element.getKeyName()?.let {
          currentEntry = ResxEntry(it)
        }
      }

      "value" -> {
        currentText = ""
        curState = State.READING_VALUE
      }

      "comment" -> {
        currentText = ""
        curState = State.READING_COMMENT
      }
    }
  }

  suspend fun SequenceScope<ResxEntry>.handleEndElement(element: EndElement) {
    val name = element.name.localPart.lowercase()
    when (name) {
      "data" -> {
        currentEntry?.let { yield(it) }
        currentEntry = null
        currentText = ""
        curState = null
      }

      "value" -> {
        if (curState == State.READING_VALUE) {
          currentEntry = currentEntry?.copy(data = currentText)
        }
        curState = null
      }

      "comment" -> {
        if (curState == State.READING_COMMENT) {
          currentEntry = currentEntry?.copy(comment = currentText)
        }
        curState = null
      }
    }
  }

  fun handleText(event: XMLEvent) {
    if (!isAnyToContentSaveOpen() || event !is Characters) {
      return
    }
    currentText += event.asCharacters().data
  }

  fun isAnyToContentSaveOpen(): Boolean = curState != null

  private fun StartElement.getKeyName() = getAttributeByName(QName(null, "name"))?.value

  enum class State {
    READING_VALUE,
    READING_COMMENT,
  }
}
