package io.tolgee.formats.xmlResources

class XmlResourcesStringsModel {
  val items: MutableMap<String, XmlResourcesNode> = mutableMapOf()
}

class StringUnit : XmlResourcesNodeWithComment {
  var value: XmlResourcesStringValue? = null
  override var comment: String? = null
}

class StringArrayUnit : XmlResourcesNode {
  val items = mutableListOf<StringArrayItem>()
}

class StringArrayItem(
  var value: XmlResourcesStringValue? = null,
  var index: Int? = null,
  override var comment: String? = null,
) : XmlResourcesNodeWithComment

class PluralUnit : XmlResourcesNodeWithComment {
  val items = mutableMapOf<String, XmlResourcesStringValue>()
  override var comment: String? = null
}

data class XmlResourcesStringValue(
  val string: String,
  val isWrappedCdata: Boolean,
)

interface XmlResourcesNodeWithComment : XmlResourcesNode {
  var comment: String?
}

interface XmlResourcesNode
