package io.tolgee.formats.xmlResources.`in`

import io.tolgee.formats.blockXmlParser.BlockXmlParser
import io.tolgee.formats.blockXmlParser.ModelCharacters
import io.tolgee.formats.blockXmlParser.ModelElement
import io.tolgee.formats.blockXmlParser.ModelNode
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import javax.xml.stream.events.XMLEvent

/**
 * This classes handlers a "value block" inside the android resources xml file.
 * Since Android have special syntax inside the values in string, string-array item or plural item, this class
 * parses this file and converts it to a value, we can store as a string in Tolgee.
 *
 * It handles:
 *  - Quoted text
 *  - escaped characters like apostrophe `'` or quotes `"`,
 *  - removes unsupported tags
 *  - replaces CDATA nodes with inner text
 */
class XmlResourcesValueBlockParser(
  private val stringUnescaper: StringUnescaper,
  private val supportedTags: Set<String>,
) {
  private val blockXmlParser = BlockXmlParser()

  fun onXmlEvent(event: XMLEvent) {
    blockXmlParser.onXmlEvent(event)
  }

  val result by lazy {
    transform()

    val singleTextResult = rootModel.getSingleTextChild()
    if (singleTextResult != null) {
      // we just need to bypass the xml escaping, since if the text doesn't contain XML, we handle it as a text,
      // which can contain characters like &, <, >
      return@lazy XmlResourcesStringValue(singleTextResult, false)
    }

    val children = rootModel.children
    val text = children.joinToString("") { it.toXmlString() }
    val singleChild = getRootSingleChild()
    val isWrappedCharacterData = singleChild is ModelCharacters && singleChild.isCdata
    XmlResourcesStringValue(text, isWrappedCharacterData)
  }

  private val rootModel get() = blockXmlParser.rootModel

  private fun transform() {
    rootModel.forEachDeep { node ->
      replaceWithTextIfUnsupported(node)
      unescapeText(node)
    }
    removeEmptyTextNodes()
  }

  private fun removeEmptyTextNodes() {
    rootModel.removeIfDeep {
      it is ModelCharacters && it.characters.isEmpty()
    }
  }

  private fun unescapeText(node: ModelNode) {
    if (node !is ModelCharacters) return
    node.characters = stringUnescaper(node.characters, node.isFirstChild, node.isLastChild)
  }

  private fun getRootSingleChild(): ModelNode? {
    if (rootModel.children.size != 1) {
      return null
    }
    return rootModel.children[0]
  }

  private fun replaceWithTextIfUnsupported(node: ModelNode) {
    if (node !is ModelElement) return
    if (node.name !in supportedTags) {
      val parentsChildren = node.parent?.children
      val index = parentsChildren?.indexOf(node) ?: return
      parentsChildren[index] = ModelCharacters(node.getText(), false, blockXmlParser.getAndIncrementId(), node.parent)
    }
  }

  private fun ModelNode.forEachDeep(block: (ModelNode) -> Unit) {
    block(this)
    (this as? ModelElement)?.let {
      it.children.forEach { element -> element.forEachDeep(block) }
    }
  }
}
