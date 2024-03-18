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
      }

      is Attribute -> {
        currentModel.attributes[event.name.localPart] = event.value
      }

      is EndElement -> {
        currentModel = currentModel.parent ?: rootModel
      }
    }
  }

  private fun transform() {
    rootModel.forEachDeep { node ->
      replaceWithTextIfUnsupported(node)
      unescapeText(node)
    }
  }

  val result by lazy {
    transform()
    val children = rootModel.children
    val text = children.joinToString("") { it.toXmlString() }
    val singleChild = getRootSingleChild()
    val isWrappedCharacterData = singleChild is ModelCharacters && singleChild.isCdata
    AndroidStringValue(text, isWrappedCharacterData)
  }

  private fun unescapeText(node: ModelNode) {
    if (node !is ModelCharacters) return
    node.characters = AndroidStringUnescaper(node.characters).unescaped
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
    val attributes: LinkedHashMap<String, String> = LinkedHashMap()
    val children: MutableList<ModelNode> = mutableListOf()

    private val stringBuilder by lazy {
      StringBuilder()
    }

    override fun toXmlString(): String {
      stringBuilder.clear()
      stringBuilder.append("<$name")
      attributes.forEach { (key, value) ->
        stringBuilder.append(" $key=\"${value.escapeXmlAttribute()}\"")
      }
      stringBuilder.append(">")
      children.forEach {
        stringBuilder.append(it.toXmlString())
      }
      stringBuilder.append("</$name>")
      return stringBuilder.toString()
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

    override fun hashCode(): Int {
      return id
    }

    abstract fun getText(): String
  }

  data class Result(val text: String, val isWrappedCdata: Boolean)
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
