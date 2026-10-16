package io.tolgee.batch.processors

import io.tolgee.batch.AbstractChunkProcessor
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.request.TagKeysRequest
import io.tolgee.model.batch.params.TagKeysParams
import io.tolgee.service.key.TagService
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.ObjectMapper
import kotlin.coroutines.CoroutineContext

@Component
class TagKeysChunkProcessor(
  private val entityManager: EntityManager,
  private val tagService: TagService,
  private val progressManager: ProgressManager,
  private val transactionManager: PlatformTransactionManager,
  objectMapper: ObjectMapper,
) : AbstractChunkProcessor<TagKeysRequest, TagKeysParams, Long>(objectMapper) {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
  ) {
    val subChunked = chunk.chunked(100)
    val params = getParams(job)

    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    createMissingTags(projectId, params.tags)

    subChunked.forEach { subChunk ->
      coroutineContext.ensureActive()
      tagService.tagKeysById(projectId, subChunk.associateWith { params.tags })
      entityManager.flush()
      progressManager.reportSingleChunkProgress(job.id, subChunk.size)
    }
  }

  /**
   * Chunks of one job run in parallel; creating a new tag inside each chunk transaction makes all but one of them
   * fail on the unique constraint and retry.
   */
  private fun createMissingTags(
    projectId: Long,
    tags: List<String>,
  ) {
    tryUntilItDoesntBreakConstraint(maxRepeats = 10) {
      executeInNewTransaction(transactionManager) {
        tagService.createMissingTags(projectId, tags)
      }
    }
  }

  override fun getTarget(data: TagKeysRequest): List<Long> {
    return data.keyIds
  }

  override fun getParamsType(): Class<TagKeysParams> {
    return TagKeysParams::class.java
  }

  override fun getTargetItemType(): Class<Long> {
    return Long::class.java
  }

  override fun getParams(data: TagKeysRequest): TagKeysParams {
    return TagKeysParams().apply {
      this.tags = data.tags
    }
  }

  override fun getChunkSize(
    request: TagKeysRequest,
    projectId: Long?,
  ): Int = 5000
}
