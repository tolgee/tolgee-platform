package io.tolgee.model.aiMatchStats

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import java.util.Date

/**
 * Watermark for the on-demand AI-match recompute: the highest `activity_revision.id` already folded
 * into [TranslationAiMatch] for a project. One row per project. When the project's max revision id
 * exceeds this, the next stats read computes only the delta and advances the watermark.
 */
@Entity
@Table
class AiMatchRefreshState(
  @Id
  @Column(name = "project_id")
  val projectId: Long,
) {
  @Column(name = "last_processed_activity_revision_id", nullable = false)
  var lastProcessedActivityRevisionId: Long = 0

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_updated_at", nullable = false)
  lateinit var lastUpdatedAt: Date
}
