package io.tolgee.formats.nestedStructureModel

interface StructuredModelItem {
  val parent: ContainerNode<*>?
  val key: Any?
}

interface ContainerNode<T : Any> :
  MutableMap<T, StructuredModelItem>,
  StructuredModelItem {
  val isPlural: Boolean
}

class ValueStructuredModelItem(
  val value: String?,
  override val parent: ContainerNode<*>?,
  override val key: Any?,
) : StructuredModelItem

class ObjectStructuredModelItem(
  override val parent: ContainerNode<*>?,
  override val key: Any?,
  /**
   * Whether this is a parent node of plural forms.
   *
   * Some formats require exporting plural forms as separate object params, e.g.
   * { "dogs count": {
   *   "one": "dog",
   *   "other": "%d dogs"
   * }
   */
  override val isPlural: Boolean = false,
) : LinkedHashMap<String, StructuredModelItem>(),
  ContainerNode<String>

class ArrayStructuredModelItem(
  override val parent: ContainerNode<*>?,
  override val key: Any?,
) : LinkedHashMap<Int, StructuredModelItem>(),
  StructuredModelItem,
  ContainerNode<Int> {
  /**
   * Whether this is a parent node of plural forms.
   *
   * We currently don't support plural forms in arrays as
   * no supported format requires this so far.
   *
   * So this is always false.
   */
  override val isPlural: Boolean = false
}
