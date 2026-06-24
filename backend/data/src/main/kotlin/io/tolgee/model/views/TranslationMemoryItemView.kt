package io.tolgee.model.views

import java.util.Date

data class TranslationMemoryItemView
  @JvmOverloads
  constructor(
    val baseTranslationText: String,
    val targetTranslationText: String,
    val keyName: String,
    val keyNamespace: String?,
    val similarity: Float,
    val keyId: Long,
    val translationMemoryName: String? = null,
    /**
     * Similarity before the TM/assignment penalty was subtracted. Equals [similarity]
     * when no penalty applies. Used by the editor panel to show a breakdown tooltip.
     */
    val rawSimilarity: Float = similarity,
    /**
     * Last update time of the underlying source. For stored entries this is the entry's
     * own `updated_at`; for virtual rows it's the contributing target translation's
     * `updated_at`, since that is what the user perceives as the "match age". Null only
     * when the row predates the column (legacy data).
     */
    val updatedAt: Date? = null,
  )
