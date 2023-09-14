package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.AutomationTargetItem
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.AutomationBjRequest
import io.tolgee.model.batch.params.AutomationBjParams
import io.tolgee.service.key.TagService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class AutomationChunkProcessor(
  private val entityManager: EntityManager,
  private val tagService: TagService
) : ChunkProcessor<AutomationBjRequest, AutomationBjParams, AutomationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<AutomationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit
  ) {
    chunk.forEach {
      coroutineContext.ensureActive()
    }
  }

  override fun getTarget(data: AutomationBjRequest): List<AutomationTargetItem> {
    return listOf(AutomationTargetItem(data.trigger.id, data.action.id))
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
