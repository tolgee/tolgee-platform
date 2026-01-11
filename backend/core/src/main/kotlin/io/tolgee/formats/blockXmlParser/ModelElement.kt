package io.tolgee.formats.blockXmlParser

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

  fun getSingleTextChild(): String? {
    val onlyTextChild = this.children.singleOrNull()
    if (onlyTextChild !is ModelCharacters || onlyTextChild.isCdata) {
      return null
    }
    return onlyTextChild.characters
  }

  fun getJoinedIfCharactersOnly(): String? {
    val allCharacters = this.children.all { it is ModelCharacters }
    if (!allCharacters) {
      return null
    }

    return this.children.joinToString("") {
      val characters = it as? ModelCharacters
      characters?.characters ?: ""
    }
  }

  override fun toXmlString(): String {
    stringBuilder.clear()
    stringBuilder.append("<$name")
    attributes.forEach { (key, value) ->
      val escaped = BlockXmlParser.escapeXmlAttribute(value)
      stringBuilder.append(" $key=\"$escaped\"")
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
      val space = if (attributes.isNotEmpty()) " " else ""
      stringBuilder.append("$space/>")
      return
    }
    stringBuilder.append(">")
  }

  override fun getText(): String {
    return children.joinToString("") { it.getText() }
  }
}
