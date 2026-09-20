package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.data.AutomationTargetItem
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.AutomationBjRequest
import io.tolgee.component.automations.AutomationRunner
import io.tolgee.component.automations.AutomationTriggerContext
import io.tolgee.model.batch.params.AutomationBjParams
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class AutomationChunkProcessor(
  private val automationRunner: AutomationRunner,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<AutomationBjRequest, AutomationBjParams, AutomationTargetItem>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<AutomationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    chunk.forEach {
      automationRunner.run(it.actionId, AutomationTriggerContext(it.activityRevisionId, it.contentDeliveryPublish))
    }
  }

  override fun getTarget(data: AutomationBjRequest): List<AutomationTargetItem> {
    return listOf(
      AutomationTargetItem(data.triggerId, data.actionId, data.activityRevisionId, data.contentDeliveryPublish),
    )
  }

  override fun getParamsType(): Class<AutomationBjParams> {
    return AutomationBjParams::class.java
  }

  override fun getTargetItemType(): Class<AutomationTargetItem> {
    return AutomationTargetItem::class.java
  }

  override fun getParams(data: AutomationBjRequest): AutomationBjParams {
    return AutomationBjParams()
  }
}
