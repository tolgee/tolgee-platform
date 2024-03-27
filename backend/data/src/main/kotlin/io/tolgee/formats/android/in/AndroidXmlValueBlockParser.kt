package io.tolgee.formats.android.`in`

import io.tolgee.formats.android.AndroidParsingConstants
import io.tolgee.formats.android.AndroidStringValue
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
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
class AndroidXmlValueBlockParser {
  fun onXmlEvent(event: XMLEvent) {
    when (event) {
      is Characters -> {
        currentModel.children.add(ModelCharacters(event.data, isCdata = event.isCData, idCounter++, currentModel))
      }

      is StartElement -> {
        val element = ModelElement(event.name.localPart, idCounter++, currentModel)
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

  val result by lazy {
    transform()

    val singleTextResult = getSingleTextResult()
    if (singleTextResult != null) {
      // we just need to bypass the xml escaping, since if the text doesn't contain XML, we handle it as a text,
      // which can contain characters like &, <, >
      return@lazy AndroidStringValue(singleTextResult, false)
    }

    val children = rootModel.children
    val text = children.joinToString("") { it.toXmlString() }
    val singleChild = getRootSingleChild()
    val isWrappedCharacterData = singleChild is ModelCharacters && singleChild.isCdata
    AndroidStringValue(text, isWrappedCharacterData)
  }

  private fun transform() {
    rootModel.forEachDeep { node ->
      replaceWithTextIfUnsupported(node)
      unescapeText(node)
    }
    removeEmptyTextNodes()
  }

  private fun getSingleTextResult(): String? {
    val onlyTextChild = rootModel.children.singleOrNull()
    if (onlyTextChild !is ModelCharacters || onlyTextChild.isCdata) {
      return null
    }
    return onlyTextChild.characters
  }

  private fun removeEmptyTextNodes() {
    rootModel.removeIfDeep {
      it is ModelCharacters && it.characters.isEmpty()
    }
  }

  private var lastStartElementLocationCharacterOffset = -1

  private fun unescapeText(node: ModelNode) {
    if (node !is ModelCharacters) return
    node.characters = AndroidStringUnescaper(node.characters, node.isFirstChild, node.isLastChild).unescaped
  }

  private fun getRootSingleChild(): ModelNode? {
    if (rootModel.children.size != 1) {
      return null
    }
    return rootModel.children[0]
  }

  private fun replaceWithTextIfUnsupported(node: ModelNode) {
    if (node !is ModelElement) return
    if (node.name !in AndroidParsingConstants.supportedTags) {
      val parentsChildren = node.parent?.children
      val index = parentsChildren?.indexOf(node) ?: return
      parentsChildren[index] = ModelCharacters(node.getText(), false, idCounter++, node.parent)
    }
  }

  private fun ModelNode.forEachDeep(block: (ModelNode) -> Unit) {
    block(this)
    (this as? ModelElement)?.let {
      it.children.forEach { element -> element.forEachDeep(block) }
    }
  }

  private var idCounter = 0
  private val rootModel = ModelElement("root", idCounter++, null)
  private var currentModel = rootModel

  class ModelElement(
    val name: String,
    id: Int,
    parent: ModelElement?,
  ) : ModelNode(id, parent) {
    var selfClosing: Boolean = false
    val attributes: LinkedHashMap<String, String> = LinkedHashMap()
    val children: MutableList<ModelNode> = mutableListOf()

    private val stringBuilder by lazy {
      StringBuilder()
    }

    fun removeIfDeep(predicate: (ModelNode) -> Boolean) {
      children.forEach { child ->
        if (child !is ModelElement) return@forEach
        child.removeIfDeep(predicate)
      }
      children.removeIf(predicate)
    }

    override fun toXmlString(): String {
      stringBuilder.clear()
      stringBuilder.append("<$name")
      attributes.forEach { (key, value) ->
        stringBuilder.append(" $key=\"${value.escapeXmlAttribute()}\"")
      }
      appendOpenTagEnd()
      children.forEach {
        stringBuilder.append(it.toXmlString())
      }
      appendCloseTag()
      return stringBuilder.toString()
    }

    private fun appendCloseTag() {
      if (!selfClosing) {
        stringBuilder.append("</$name>")
      }
    }

    private fun appendOpenTagEnd() {
      if (selfClosing) {
        stringBuilder.append("/>")
        return
      }
      stringBuilder.append(">")
    }

    override fun getText(): String {
      return children.joinToString("") { it.getText() }
    }
  }

  class ModelCharacters(
    var characters: String,
    val isCdata: Boolean,
    id: Int,
    parent: ModelElement?,
  ) : ModelNode(id, parent) {
    override fun toXmlString(): String {
      if (isCdata) {
        return characters
      }
      return characters.escapeXml()
    }

    override fun getText(): String {
      return characters
    }
  }

  abstract class ModelNode(
    val id: Int,
    val parent: ModelElement?,
  ) {
    abstract fun toXmlString(): String

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as ModelNode

      return id == other.id
    }

    val isFirstChild: Boolean
      get() = parent?.children?.firstOrNull() === this

    val isLastChild: Boolean
      get() = parent?.children?.lastOrNull() === this

    override fun hashCode(): Int {
      return id
    }

    abstract fun getText(): String
  }
}

private fun String.escapeXml(): String {
  return replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
}

private fun String.escapeXmlAttribute(): String {
  return this.escapeXml()
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
}
