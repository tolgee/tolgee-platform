package io.tolgee.ee.component.contentDelivery

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.WebhookDispatchBjRequest
import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData
import io.tolgee.component.automations.processors.WebhookEventType
import io.tolgee.events.OnContentDeliveryPublished
import io.tolgee.model.Project
import io.tolgee.repository.WebhookConfigRepository
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ContentDeliveryPublishWebhookListener(
  private val webhookConfigRepository: WebhookConfigRepository,
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  @Lazy private val self: ContentDeliveryPublishWebhookListener,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  @Async
  fun onContentDeliveryPublished(event: OnContentDeliveryPublished) {
    self.enqueueWebhooks(event.data)
  }

  @Transactional
  fun enqueueWebhooks(data: ContentDeliveryPublishWebhookData) {
    val webhooks =
      webhookConfigRepository
        .findAllByProjectId(data.projectId)
        .filter { it.enabled && it.eventTypes.contains(WebhookEventType.CONTENT_DELIVERY_PUBLISH) }
    if (webhooks.isEmpty()) return

    val project = entityManager.getReference(Project::class.java, data.projectId)
    webhooks.forEach { webhook ->
      batchJobService.startJob(
        request = WebhookDispatchBjRequest(webhook.id, data.projectId, data),
        project = project,
        author = null,
        type = BatchJobType.WEBHOOK_DISPATCH,
        isHidden = true,
      )
    }
  }
}
