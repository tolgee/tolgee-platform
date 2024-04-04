package io.tolgee.formats.android

class AndroidStringsXmlModel {
  val items: MutableMap<String, AndroidXmlNode> = mutableMapOf()
}

class StringUnit : AndroidXmlNode {
  var value: AndroidStringValue? = null
}

class StringArrayUnit : AndroidXmlNode {
  val items = mutableListOf<StringArrayItem>()
}

class StringArrayItem(
  var value: AndroidStringValue? = null,
  var index: Int? = null,
) : AndroidXmlNode

class PluralUnit : AndroidXmlNode {
  val items = mutableMapOf<String, AndroidStringValue>()
}

data class AndroidStringValue(val string: String, val isWrappedCdata: Boolean)

interface AndroidXmlNode
