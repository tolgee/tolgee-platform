package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.AiReviewDemotionRequest
import io.tolgee.service.aiReview.AiReviewDemotionService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class AiReviewDemotionChunkProcessor(
  private val aiReviewDemotionService: AiReviewDemotionService,
  private val progressManager: ProgressManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<AiReviewDemotionRequest, Any?, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    coroutineContext.ensureActive()
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    aiReviewDemotionService.demoteKeys(projectId, chunk)
    progressManager.reportSingleChunkProgress(job.id, chunk.size)
  }

  override fun getTarget(data: AiReviewDemotionRequest): List<Long> = data.keyIds

  override fun getParams(data: AiReviewDemotionRequest): Any? = null

  override fun getParamsType(): Class<Any?>? = null

  override fun getTargetItemType(): Class<Long> = Long::class.java

  override fun getChunkSize(
    request: AiReviewDemotionRequest,
    projectId: Long?,
  ): Int = 100

  override fun getJobCharacter(
    request: AiReviewDemotionRequest,
    projectId: Long?,
  ): JobCharacter = JobCharacter.FAST
}
