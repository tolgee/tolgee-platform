package io.tolgee.formats.blockXmlParser

import javax.xml.stream.events.Attribute
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class BlockXmlParser {
  fun onXmlEvent(event: XMLEvent) {
    when (event) {
      is Characters -> {
        currentModel.children.add(
          ModelCharacters(
            event.data,
            isCdata = event.isCData,
            idCounter++,
            currentModel,
          ),
        )
      }

      is StartElement -> {
        val element = ModelElement(event.name.localPart, getAndIncrementId(), currentModel)
        currentModel.children.add(element)
        currentModel = element
        lastStartElementLocationCharacterOffset = event.location.characterOffset
      }

      is Attribute -> {
        currentModel.attributes[event.name.localPart] = event.value
      }

      is EndElement -> {
        currentModel.selfClosing = lastStartElementLocationCharacterOffset == event.location.characterOffset
        currentModel = currentModel.parent ?: rootModel
      }
    }
  }

  private var idCounter = 0
  val rootModel = ModelElement("root", idCounter++, null)
  private var currentModel = rootModel
  private var lastStartElementLocationCharacterOffset = -1

  fun getAndIncrementId() = idCounter++

  companion object {
    fun escapeXml(s: String): String {
      return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    }

    fun escapeXmlAttribute(s: String): String {
      return escapeXml(s)
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
    }
  }
}
