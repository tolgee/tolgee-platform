package io.tolgee.batch.data

/**
 * Every (key, language) combination of [keyIds] and [languageIds], materialized lazily.
 *
 * Jobs with this target store only the two id lists ([TranslationTargetProductSpec]) instead of one json item
 * per combination, which keeps the stored target small (a json value is limited to 256 MB) and lets chunks be
 * sliced without building the whole list.
 */
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
  /** All languages of the first key, then all languages of the second key... */
  KEY_MAJOR,

  /** All keys of the first language, then all keys of the second language... */
  LANGUAGE_MAJOR,
}

data class TranslationTargetProductSpec(
  var keyIds: List<Long> = listOf(),
  var languageIds: List<Long> = listOf(),
  var order: TranslationTargetProductOrder = TranslationTargetProductOrder.KEY_MAJOR,
) {
  fun toTarget() = TranslationTargetProduct(keyIds, languageIds, order)
}
