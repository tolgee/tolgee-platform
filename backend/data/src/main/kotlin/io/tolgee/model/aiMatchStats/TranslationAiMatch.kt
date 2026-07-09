package io.tolgee.model.aiMatchStats

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.util.Date

/**
 * Materialized AI-match score for one reviewed, AI-originated translation.
 *
 * Derived (not source-of-truth) data: recomputed from the translation's activity history by
 * [io.tolgee.service.aiMatchStats.AiMatchStatsService]. Keyed by `translationId` (one row per
 * translation); a translation that stops qualifying (un-reviewed, edited away from AI origin, or
 * deleted) has its row removed on recompute.
 */
@Entity
@Table(
  indexes = [
    Index(columnList = "project_id,reviewed_at"),
    Index(columnList = "project_id,language_id"),
    Index(columnList = "project_id,prompt_id,prompt_version_ts"),
  ],
)
class TranslationAiMatch(
  @Id
  @Column(name = "translation_id")
  val translationId: Long,
) {
  @Column(name = "project_id", nullable = false)
  var projectId: Long = 0

  @Column(name = "language_id", nullable = false)
  var languageId: Long = 0

  /** 0-100, see [io.tolgee.service.aiMatchStats.AiMatchScorer]. */
  @Column(name = "match_score", nullable = false)
  var matchScore: Int = 0

  /** Timestamp of the last `→ REVIEWED` transition — the value the range filter compares against. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "reviewed_at", nullable = false)
  lateinit var reviewedAt: Date

  /** Word count of the current (reviewed) text — the aggregation weight. */
  @Column(name = "word_count", nullable = false)
  var wordCount: Int = 0

  /** Prompt that produced the scored AI text; null for MT engines and the built-in default prompt. */
  @Column(name = "prompt_id")
  var promptId: Long? = null

  /** [io.tolgee.constants.MtServiceType] name of the scored AI revision (e.g. PROMPT, GOOGLE). */
  @Column(name = "mt_provider")
  var mtProvider: String? = null

  /** Prompt version live when the AI text was produced; null for default/MT (unversioned). */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "prompt_version_ts")
  var promptVersionTs: Date? = null
}
