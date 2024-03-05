package io.tolgee.formats.android

class AndroidStringsXmlModel {
  val items: MutableMap<String, AndroidXmlNode> = mutableMapOf()
}

class StringUnit : AndroidXmlNode {
  var value: String? = null
}

class StringArrayUnit : AndroidXmlNode {
  val items = mutableListOf<StringArrayItem>()
}

class StringArrayItem(
  var value: String? = null,
  var index: Int? = null,
) : AndroidXmlNode

class PluralUnit : AndroidXmlNode {
  val items = mutableMapOf<String, String>()
}

interface AndroidXmlNode
