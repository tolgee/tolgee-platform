package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.MtProviderCatching
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.model.batch.params.AiReviewJobParams
import io.tolgee.service.aiReview.AiReviewBatchService
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class AiReviewChunkProcessor(
  private val aiReviewBatchService: AiReviewBatchService,
  private val mtProviderCatching: MtProviderCatching,
  private val progressManager: ProgressManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<AiReviewRequest, AiReviewJobParams, BatchTranslationTargetItem>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    val params = getParams(job)
    mtProviderCatching.iterateCatching(chunk, coroutineContext) { item ->
      processItem(projectId, item, params)
      progressManager.reportSingleChunkProgress(job.id)
    }
  }

  private fun processItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
    params: AiReviewJobParams,
  ) {
    if (params.reevaluateOnly) {
      aiReviewBatchService.reevaluateItem(projectId, item)
      return
    }
    aiReviewBatchService.reviewItem(projectId, item)
  }

  override fun getTarget(data: AiReviewRequest): List<BatchTranslationTargetItem> = data.target

  override fun getParams(data: AiReviewRequest): AiReviewJobParams =
    AiReviewJobParams().apply {
      reevaluateOnly = data.reevaluateOnly
      handlingStuckItems = data.handlingStuckItems
    }

  override fun getParamsType(): Class<AiReviewJobParams> = AiReviewJobParams::class.java

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> = BatchTranslationTargetItem::class.java

  override fun getChunkSize(
    request: AiReviewRequest,
    projectId: Long?,
  ): Int = 1

  override fun getJobCharacter(
    request: AiReviewRequest,
    projectId: Long?,
  ): JobCharacter = JobCharacter.SLOW
}
