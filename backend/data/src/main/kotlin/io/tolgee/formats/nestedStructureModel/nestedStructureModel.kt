package io.tolgee.formats.nestedStructureModel

interface StructuredModelItem {
  val parent: ContainerNode<*>?
  val key: Any?
}

interface ContainerNode<T : Any> : MutableMap<T, StructuredModelItem>, StructuredModelItem {
  val isPlural: Boolean
}

class ValueStructuredModelItem(
  val value: String?,
  override val parent: ContainerNode<*>?,
  override val key: Any?,
) :
  StructuredModelItem

class ObjectStructuredModelItem(
  override val parent: ContainerNode<*>?,
  override val key: Any?,
  override val isPlural: Boolean = false,
) :
  LinkedHashMap<String, StructuredModelItem>(), ContainerNode<String>

class ArrayStructuredModelItem(
  override val parent: ContainerNode<*>?,
  override val key: Any?,
) : LinkedHashMap<Int, StructuredModelItem>(),
  StructuredModelItem,
  ContainerNode<Int> {
  override val isPlural: Boolean = false
}
