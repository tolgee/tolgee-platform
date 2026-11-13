package io.tolgee.ee.service.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.service.aiReview.AiReviewQueued
import io.tolgee.service.aiReview.AiReviewResultApplies
import io.tolgee.websocket.ActivityWebsocketListener
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import io.tolgee.websocket.WebsocketEventType
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

data class AiReviewUpdate(
  val translationId: Long,
  val keyId: Long,
  val languageTag: String,
  /** Null means the review row was removed or no longer applies (AiReviewResultApplies). */
  val status: AiReviewStatus?,
  val score: Int?,
  val scoreOnlyRun: Boolean,
  val lastFailed: Boolean,
  val queued: Boolean,
) {
  companion object {
    fun of(
      row: TranslationAiReview,
      scoreOnlyRun: Boolean,
    ) = of(
      row.translation,
      row.status,
      row.score,
      scoreOnlyRun,
      lastFailed = row.lastFailureReason != null,
      queued = AiReviewQueued.of(row),
    )

    fun of(
      translation: Translation,
      status: AiReviewStatus?,
      score: Int?,
      scoreOnlyRun: Boolean,
      lastFailed: Boolean,
      queued: Boolean,
    ): AiReviewUpdate {
      if (status == null) return removed(translation)
      if (!AiReviewResultApplies.stillApplies(status, translation.state)) return removed(translation)
      return AiReviewUpdate(
        translationId = translation.id,
        keyId = translation.key.id,
        languageTag = translation.language.tag,
        status = status,
        score = score,
        scoreOnlyRun = scoreOnlyRun,
        lastFailed = lastFailed,
        queued = queued,
      )
    }

    fun removed(translation: Translation) =
      AiReviewUpdate(
        translationId = translation.id,
        keyId = translation.key.id,
        languageTag = translation.language.tag,
        status = null,
        score = null,
        scoreOnlyRun = false,
        lastFailed = false,
        queued = false,
      )

    fun scoreOnlyRun(row: TranslationAiReview): Boolean =
      row.ruleMode == AiReviewRuleMode.SCORE_ONLY ||
        row.status == AiReviewStatus.SCORE_ONLY_PASS ||
        row.status == AiReviewStatus.SCORE_ONLY_FAIL
  }
}

@Component
class AiReviewWebsocketPublisher(
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun publishAfterCommit(
    projectId: Long,
    updates: List<AiReviewUpdate>,
  ) {
    if (updates.isEmpty()) return
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      publish(projectId, updates)
      return
    }
    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun afterCommit() {
          publish(projectId, updates)
        }
      },
    )
  }

  private fun publish(
    projectId: Long,
    updates: List<AiReviewUpdate>,
  ) {
    websocketEventPublisher(
      WebsocketEventType.AI_REVIEW_UPDATED.projectDestinationFor(projectId),
      when {
        updates.size >= ActivityWebsocketListener.COLLAPSE_LIMIT ->
          WebsocketEvent(
            data = null,
            dataCollapsed = true,
            timestamp = currentDateProvider.date.time,
          )
        else ->
          WebsocketEvent(
            data = updates,
            timestamp = currentDateProvider.date.time,
          )
      },
    )
  }
}
