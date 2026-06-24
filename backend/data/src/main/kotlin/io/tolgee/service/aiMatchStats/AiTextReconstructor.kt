package io.tolgee.service.aiMatchStats

import java.util.Date

/**
 * Reconstructs, from a single translation's ordered activity history, the AI output the reviewer
 * actually saw — the most recent AI-produced text at or before the last `→ REVIEWED` transition.
 *
 * This walk is required (rather than reading the live row) because [io.tolgee.model.translation.Translation.resetFlags]
 * clears `auto`/`mtProvider`/`promptId` once a reviewer edits, so the AI origin survives only here.
 */
object AiTextReconstructor {
  data class AiSource(
    val promptId: Long?,
    val mtProvider: String?,
    val producedAt: Date,
  )

  data class WalkResult(
    val aiText: String?,
    val reviewedAt: Date?,
    val sawReview: Boolean,
    val aiSource: AiSource?,
  )

  /**
   * @param rows the translation's activity revisions, oldest first.
   */
  fun walk(rows: List<AiMatchActivityRow>): WalkResult {
    var currentText = ""
    var lastAiText: String? = null
    var lastAi: AiSource? = null
    var aiTextAtReview: String? = null
    var aiAtReview: AiSource? = null
    var reviewedAt: Date? = null
    var sawReview = false

    for (row in rows) {
      if (row.textModified && row.textNew != null) {
        currentText = row.textNew
      }
      if (isAiRevision(row)) {
        lastAiText = currentText
        lastAi = AiSource(row.promptIdNew, row.mtProviderNew, row.timestamp)
      }
      if (row.stateNew == REVIEWED) {
        reviewedAt = row.timestamp
        aiTextAtReview = lastAiText
        aiAtReview = lastAi
        sawReview = true
      }
    }

    if (sawReview) {
      return WalkResult(aiTextAtReview, reviewedAt, true, aiAtReview)
    }
    return WalkResult(lastAiText, rows.lastOrNull()?.timestamp, false, lastAi)
  }

  private fun isAiRevision(row: AiMatchActivityRow): Boolean {
    if (row.autoNew == true) return true
    if (!row.mtProviderNew.isNullOrBlank()) return true
    if (row.promptIdNew != null) return true
    return false
  }

  private const val REVIEWED = "REVIEWED"
}
