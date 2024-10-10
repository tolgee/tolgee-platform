package io.tolgee.formats.android

class AndroidStringsXmlModel {
  val items: MutableMap<String, AndroidXmlNode> = mutableMapOf()
}

class StringUnit : AndroidXmlNodeWithComment {
  var value: AndroidStringValue? = null
  override var comment: String? = null
}

class StringArrayUnit : AndroidXmlNode {
  val items = mutableListOf<StringArrayItem>()
}

class StringArrayItem(
  var value: AndroidStringValue? = null,
  var index: Int? = null,
  override var comment: String? = null,
) : AndroidXmlNodeWithComment

class PluralUnit : AndroidXmlNodeWithComment {
  val items = mutableMapOf<String, AndroidStringValue>()
  override var comment: String? = null
}

data class AndroidStringValue(val string: String, val isWrappedCdata: Boolean)

interface AndroidXmlNodeWithComment : AndroidXmlNode {
  var comment: String?
}

interface AndroidXmlNode
