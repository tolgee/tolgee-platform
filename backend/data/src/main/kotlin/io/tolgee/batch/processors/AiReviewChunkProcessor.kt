package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.ChunkItemFailedException
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.LlmRateLimitedException
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
  private val currentDateProvider: CurrentDateProvider,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<AiReviewRequest, AiReviewJobParams, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val successfulTargets = mutableListOf<Long>()
    chunk.forEach { translationId ->
      coroutineContext.ensureActive()
      try {
        aiReviewBatchService.reviewTranslation(job, translationId)
      } catch (e: LlmRateLimitedException) {
        throw ChunkItemFailedException(
          Message.LLM_RATE_LIMITED,
          successfulTargets,
          e,
          (e.retryAt - currentDateProvider.date.time).toInt(),
          increaseFactor = 1,
          maxRetries = -1,
        )
      }
      successfulTargets += translationId
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
