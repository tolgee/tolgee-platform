package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.model.batch.params.QaCheckJobParams
import io.tolgee.service.qa.QaCheckBatchService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class QaCheckChunkProcessor(
  private val qaCheckBatchService: QaCheckBatchService,
  private val progressManager: ProgressManager,
) : ChunkProcessor<QaCheckRequest, QaCheckJobParams, Long> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val params = getParams(job)
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    chunk.forEach { translationId ->
      coroutineContext.ensureActive()
      qaCheckBatchService.runChecksAndPersist(projectId, translationId, params.checkTypes)
      progressManager.reportSingleChunkProgress(job.id)
    }
  }

  override fun getTarget(data: QaCheckRequest): List<Long> = data.translationIds

  override fun getParams(data: QaCheckRequest): QaCheckJobParams =
    QaCheckJobParams().apply {
      checkTypes = data.checkTypes
    }

  override fun getParamsType(): Class<QaCheckJobParams> = QaCheckJobParams::class.java

  override fun getTargetItemType(): Class<Long> = Long::class.java

  override fun getChunkSize(
    request: QaCheckRequest,
    projectId: Long?,
  ): Int = 10

  override fun getJobCharacter(): JobCharacter = JobCharacter.SLOW
}
