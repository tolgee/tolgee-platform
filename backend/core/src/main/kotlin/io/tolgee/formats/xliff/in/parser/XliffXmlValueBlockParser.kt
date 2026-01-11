package io.tolgee.formats.xliff.`in`.parser

import io.tolgee.formats.blockXmlParser.BlockXmlParser
import javax.xml.stream.events.XMLEvent

class XliffXmlValueBlockParser {
  private val blockXmlParser = BlockXmlParser()

  fun onXmlEvent(event: XMLEvent) {
    blockXmlParser.onXmlEvent(event)
  }

  val result by lazy {
    val joinedCharacters = rootModel.getJoinedIfCharactersOnly()
    if (joinedCharacters != null) {
      return@lazy joinedCharacters
    }

    val children = rootModel.children
    val text = children.joinToString("") { it.toXmlString() }
    text
  }

  private val rootModel get() = blockXmlParser.rootModel
}
