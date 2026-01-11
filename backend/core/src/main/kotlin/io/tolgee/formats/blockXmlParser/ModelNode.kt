package io.tolgee.formats.blockXmlParser

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
