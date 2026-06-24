package io.tolgee.service.aiMatchStats

import java.util.Date

/** Live state of a translation, used to decide whether (and how) to materialize its match. */
data class CurrentTranslationRow(
  val text: String?,
  val wordCount: Int,
  val state: Int,
  val languageId: Long,
)

/** A computed materialized row, ready for a bulk upsert into `translation_ai_match`. */
data class TranslationAiMatchRow(
  val translationId: Long,
  val projectId: Long,
  val languageId: Long,
  val matchScore: Int,
  val reviewedAt: Date,
  val wordCount: Int,
  val promptId: Long?,
  val mtProvider: String?,
  val promptVersionTs: Date?,
)

/** Per-language reviewed-AI aggregate, word-weighted, read from the materialized table. */
data class ReviewedLangAgg(
  val languageId: Long,
  val reviewedKeys: Long,
  val reviewedWords: Long,
  val weightedScoreSum: Long,
  val b100Words: Long,
  val b9990Words: Long,
  val b8980Words: Long,
  val b7970Words: Long,
  val bnoWords: Long,
  val b100Keys: Long,
  val b9990Keys: Long,
  val b8980Keys: Long,
  val b7970Keys: Long,
  val bnoKeys: Long,
)

/** Per-language count of AI cells still awaiting review (live, current state). */
data class NotReviewedAgg(
  val languageId: Long,
  val keys: Long,
  val words: Long,
)

/**
 * Per-(prompt, provider) count of AI cells still awaiting review (live, current state). Not split by
 * prompt version — a not-yet-reviewed cell has no review timestamp to attribute a version from.
 */
data class NotReviewedPromptAgg(
  val promptId: Long?,
  val mtProvider: String?,
  val promptName: String?,
  val keys: Long,
  val words: Long,
)

/** Per-(prompt, version) reviewed-AI aggregate. */
data class PromptAgg(
  val promptId: Long?,
  val mtProvider: String?,
  val promptVersionTs: Date?,
  val promptName: String?,
  val reviewedKeys: Long,
  val reviewedWords: Long,
  val weightedScoreSum: Long,
  val b100Words: Long,
  val b9990Words: Long,
  val b8980Words: Long,
  val b7970Words: Long,
  val bnoWords: Long,
)
