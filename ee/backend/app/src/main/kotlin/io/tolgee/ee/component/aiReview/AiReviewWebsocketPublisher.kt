package io.tolgee.ee.component.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.service.aiReview.AiReviewUpdatedEvent
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import io.tolgee.websocket.WebsocketEventType
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AiReviewWebsocketPublisher(
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val currentDateProvider: CurrentDateProvider,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  fun onAiReviewUpdated(event: AiReviewUpdatedEvent) {
    websocketEventPublisher(
      WebsocketEventType.AI_REVIEW_UPDATED.projectDestinationFor(event.projectId),
      WebsocketEvent(
        data =
          listOf(
            mapOf(
              "translationId" to event.translationId,
              "keyId" to event.keyId,
              "languageTag" to event.languageTag,
              "state" to event.state,
              "aiReview" to getSummary(event),
            ),
          ),
        timestamp = currentDateProvider.date.time,
      ),
    )
  }

  private fun getSummary(event: AiReviewUpdatedEvent): Map<String, Any?>? {
    if (!event.outcome.isShownFor(event.state)) return null
    return mapOf(
      "outcome" to event.outcome,
      "score" to event.score,
      "threshold" to event.threshold,
      "dryRun" to event.dryRun,
    )
  }
}
