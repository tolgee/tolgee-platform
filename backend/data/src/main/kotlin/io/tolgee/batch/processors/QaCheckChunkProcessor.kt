package io.tolgee.batch.processors

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
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
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<QaCheckRequest, QaCheckJobParams, BatchTranslationTargetItem>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    val params = getParams(job)
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    val isSlow = params.checkTypes?.any { it.isSlow } ?: true

    coroutineContext.ensureActive()

    qaCheckBatchService.runChecksAndPersistChunk(
      projectId = projectId,
      checkTypes = params.checkTypes,
      items = chunk,
    ) {
      // progress callback
      coroutineContext.ensureActive()
      if (isSlow) {
        progressManager.reportSingleChunkProgress(job.id)
      }
    }
  }

  override fun getTarget(data: QaCheckRequest): List<BatchTranslationTargetItem> = data.target

  override fun getParams(data: QaCheckRequest): QaCheckJobParams =
    QaCheckJobParams().apply {
      checkTypes = data.checkTypes
      handlingStuckStaleItems = data.handlingStuckStaleItems
    }

  override fun getParamsType(): Class<QaCheckJobParams> = QaCheckJobParams::class.java

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> = BatchTranslationTargetItem::class.java

  override fun getChunkSize(
    request: QaCheckRequest,
    projectId: Long?,
  ): Int = 100

  override fun getJobCharacter(
    request: QaCheckRequest,
    projectId: Long?,
  ): JobCharacter {
    val smallJob = request.target.size <= 10
    return if (smallJob) JobCharacter.FAST else JobCharacter.SLOW
  }
}
