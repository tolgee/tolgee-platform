package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.ScheduledServerTaskTargetItem
import io.tolgee.batch.serverTasks.ServerTaskRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.*
import kotlin.coroutines.CoroutineContext

@Component
class ScheduledServerTaskChunkProcessor(
  private val applicationContext: ApplicationContext
) : ChunkProcessor<ScheduledServerTaskTargetItem, ScheduledServerTaskTargetItem, ScheduledServerTaskTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<ScheduledServerTaskTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    chunk.forEach {
      val bean = applicationContext.getBean(it.jobBean)
      if (bean !is ServerTaskRunner) {
        throw RuntimeException("Bean ${it.jobBean} is not instance of ServerTaskRunner")
      }

      bean.execute(it.data)
    }
  }

  override fun getTarget(data: ScheduledServerTaskTargetItem): List<ScheduledServerTaskTargetItem> {
    return listOf(data)
  }

  override fun getExecuteAfter(data: ScheduledServerTaskTargetItem): Date? {
    return data.executeAfter
  }

  override fun getParamsType(): Class<ScheduledServerTaskTargetItem> {
    return ScheduledServerTaskTargetItem::class.java
  }

  override fun getTargetItemType(): Class<ScheduledServerTaskTargetItem> {
    return ScheduledServerTaskTargetItem::class.java
  }

  override fun getParams(data: ScheduledServerTaskTargetItem): ScheduledServerTaskTargetItem {
    return data
  }
}
