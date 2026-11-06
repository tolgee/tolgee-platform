package io.tolgee.ee.service.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.enums.aiReview.AiReviewStatus
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
  /** Null means the review row was removed. */
  val status: AiReviewStatus?,
  val score: Int?,
  val dryRun: Boolean,
  val lastFailed: Boolean,
)

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
      WebsocketEvent(
        data = updates,
        timestamp = currentDateProvider.date.time,
      ),
    )
  }
}
