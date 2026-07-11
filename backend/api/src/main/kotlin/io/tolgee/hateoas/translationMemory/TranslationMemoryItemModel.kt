package io.tolgee.hateoas.translationMemory

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Relation(collectionRelation = "translationMemoryItems", itemRelation = "translationMemoryItem")
open class TranslationMemoryItemModel(
  var targetText: String,
  var baseText: String,
  var keyName: String,
  var similarity: Float,
  var translationMemoryName: String? = null,
  /**
   * Similarity before the TM penalty was applied. Equals [similarity] when no penalty
   * was subtracted. Consumed by the editor to show a `raw% − penalty` breakdown tooltip.
   */
  var rawSimilarity: Float = similarity,
  /**
   * Last update timestamp of the match's underlying source — the stored entry's own
   * `updated_at` for shared/manual rows, or the contributing translation's `updated_at`
   * for virtual project-TM rows. The editor renders this as a relative "X ago" label
   * in the suggestion meta line.
   */
  var updatedAt: Date? = null,
) : RepresentationModel<TranslationMemoryItemModel>()
