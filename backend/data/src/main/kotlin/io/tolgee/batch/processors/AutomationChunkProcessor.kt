package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.AutomationTargetItem
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.AutomationBjRequest
import io.tolgee.component.automations.AutomationRunner
import io.tolgee.model.batch.params.AutomationBjParams
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class AutomationChunkProcessor(
  private val automationRunner: AutomationRunner,
) : ChunkProcessor<AutomationBjRequest, AutomationBjParams, AutomationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<AutomationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    chunk.forEach {
      automationRunner.run(it.actionId, it.activityRevisionId)
    }
  }

  override fun getTarget(data: AutomationBjRequest): List<AutomationTargetItem> {
    return listOf(AutomationTargetItem(data.triggerId, data.actionId, data.activityRevisionId))
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
