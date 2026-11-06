package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.model.batch.params.AiReviewJobParams
import io.tolgee.service.aiReview.AiReviewBatchService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class AiReviewChunkProcessor(
  private val aiReviewBatchService: AiReviewBatchService,
  private val progressManager: ProgressManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<AiReviewRequest, AiReviewJobParams, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    chunk.forEach { translationId ->
      coroutineContext.ensureActive()
      aiReviewBatchService.reviewTranslation(job, translationId)
      progressManager.reportSingleChunkProgress(job.id)
    }
  }

  override fun getTarget(data: AiReviewRequest): List<Long> = data.translationIds

  override fun getParams(data: AiReviewRequest): AiReviewJobParams =
    AiReviewJobParams().apply {
      handlingStuckItems = data.handlingStuckItems
    }

  override fun getParamsType(): Class<AiReviewJobParams> = AiReviewJobParams::class.java

  override fun getTargetItemType(): Class<Long> = Long::class.java

  override fun getChunkSize(
    request: AiReviewRequest,
    projectId: Long?,
  ): Int = 5

  override fun getJobCharacter(
    request: AiReviewRequest,
    projectId: Long?,
  ): JobCharacter = JobCharacter.SLOW
}
