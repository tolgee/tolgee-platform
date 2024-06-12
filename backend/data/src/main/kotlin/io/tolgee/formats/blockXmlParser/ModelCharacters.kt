package io.tolgee.formats.blockXmlParser

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
    return BlockXmlParser.escapeXml(characters)
  }

  override fun getText(): String {
    return characters
  }
}
