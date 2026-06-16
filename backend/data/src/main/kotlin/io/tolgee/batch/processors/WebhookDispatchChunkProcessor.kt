package io.tolgee.batch.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.WebhookDispatchBjRequest
import io.tolgee.component.automations.processors.WebhookDeliveryManager
import io.tolgee.component.automations.processors.WebhookEventType
import io.tolgee.component.automations.processors.WebhookRequest
import io.tolgee.model.batch.params.WebhookDispatchJobParams
import io.tolgee.repository.WebhookConfigRepository
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class WebhookDispatchChunkProcessor(
  private val webhookConfigRepository: WebhookConfigRepository,
  private val webhookDeliveryManager: WebhookDeliveryManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<WebhookDispatchBjRequest, WebhookDispatchJobParams, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val params = getParams(job)
    chunk.forEach { webhookConfigId ->
      val config = webhookConfigRepository.findById(webhookConfigId).orElse(null) ?: return@forEach
      val data =
        WebhookRequest(
          webhookConfigId = config.id,
          projectId = params.projectId,
          eventType = WebhookEventType.CONTENT_DELIVERY_PUBLISH,
          activityData = null,
          contentDeliveryConfig = params.data,
        )
      webhookDeliveryManager.signExecuteAndHandle(config, data)
    }
  }

  override fun getTarget(data: WebhookDispatchBjRequest): List<Long> = listOf(data.webhookConfigId)

  override fun getParamsType(): Class<WebhookDispatchJobParams> = WebhookDispatchJobParams::class.java

  override fun getTargetItemType(): Class<Long> = Long::class.java

  override fun getParams(data: WebhookDispatchBjRequest): WebhookDispatchJobParams =
    WebhookDispatchJobParams().apply {
      projectId = data.projectId
      this.data = data.data
    }

  override fun getChunkSize(
    request: WebhookDispatchBjRequest,
    projectId: Long?,
  ): Int = 1
}
