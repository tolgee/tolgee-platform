package io.tolgee.batch.data

class TranslationTargetProduct(
  val keyIds: List<Long>,
  val languageIds: List<Long>,
  val order: TranslationTargetProductOrder = TranslationTargetProductOrder.KEY_MAJOR,
) : AbstractList<BatchTranslationTargetItem>(),
  RandomAccess {
  override val size: Int = Math.multiplyExact(keyIds.size, languageIds.size)

  override fun get(index: Int): BatchTranslationTargetItem {
    if (index < 0 || index >= size) {
      throw IndexOutOfBoundsException("Index $index out of bounds for length $size")
    }
    return when (order) {
      TranslationTargetProductOrder.KEY_MAJOR ->
        BatchTranslationTargetItem(keyIds[index / languageIds.size], languageIds[index % languageIds.size])

      TranslationTargetProductOrder.LANGUAGE_MAJOR ->
        BatchTranslationTargetItem(keyIds[index % keyIds.size], languageIds[index / keyIds.size])
    }
  }

  val spec: TranslationTargetProductSpec
    get() = TranslationTargetProductSpec(keyIds, languageIds, order)
}

enum class TranslationTargetProductOrder {
  KEY_MAJOR,
  LANGUAGE_MAJOR,
}

data class TranslationTargetProductSpec(
  var keyIds: List<Long> = listOf(),
  var languageIds: List<Long> = listOf(),
  var order: TranslationTargetProductOrder = TranslationTargetProductOrder.KEY_MAJOR,
) {
  fun toTarget() = TranslationTargetProduct(keyIds, languageIds, order)
}
