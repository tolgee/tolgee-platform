package io.tolgee.batch.processors

import io.tolgee.batch.BatchApiResultHandler
import io.tolgee.batch.BatchApiSubmitter
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.batch.params.MachineTranslationJobParams
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class MachineTranslationChunkProcessor(
  private val genericAutoTranslationChunkProcessor: GenericAutoTranslationChunkProcessor,
  private val mtServiceConfigService: MtServiceConfigService,
  private val batchProperties: BatchProperties,
  private val entityManager: EntityManager,
  @Lazy
  private val batchApiSubmitter: BatchApiSubmitter?,
  @Lazy
  private val batchApiResultHandler: BatchApiResultHandler?,
  @Lazy
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository?,
) : ChunkProcessor<MachineTranslationRequest, MachineTranslationJobParams, BatchTranslationTargetItem>,
  Logging {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    val params = getParams(job)

    if (params.useBatchApi) {
      processBatchApi(job, chunk)
      return
    }

    @Suppress("UNCHECKED_CAST")
    genericAutoTranslationChunkProcessor.process(
      job,
      chunk,
      coroutineContext,
      useMachineTranslation = true,
      useTranslationMemory = false,
    )
  }

  private fun processBatchApi(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
  ) {
    val chunkExecution = findCurrentChunkExecution(job)

    // Check if we have results ready (Phase 2 - apply results)
    val tracker = openAiBatchJobTrackerRepository?.findByChunkExecutionId(chunkExecution.id)
    if (tracker != null && tracker.status == OpenAiBatchTrackerStatus.RESULTS_READY) {
      logger.debug("Phase 2: Applying batch API results for job ${job.id}, execution ${chunkExecution.id}")
      val resultHandler =
        batchApiResultHandler
          ?: throw IllegalStateException("BatchApiResultHandler not available (EE module required)")
      resultHandler.applyResults(job, chunkExecution.id)
      return
    }

    // Phase 1 - submit batch
    logger.debug("Phase 1: Submitting batch API request for job ${job.id}, execution ${chunkExecution.id}")
    val submitter =
      batchApiSubmitter
        ?: throw IllegalStateException("BatchApiSubmitter not available (EE module required)")
    submitter.submitBatch(job, chunk, chunkExecution)
  }

  @Suppress("UNCHECKED_CAST")
  private fun findCurrentChunkExecution(job: BatchJobDto): BatchJobChunkExecution {
    val executions =
      entityManager
        .createQuery(
          """
          from BatchJobChunkExecution bjce
          where bjce.batchJob.id = :jobId
          and bjce.status = :status
          order by bjce.id desc
          """.trimIndent(),
          BatchJobChunkExecution::class.java,
        ).setParameter("jobId", job.id)
        .setParameter("status", BatchJobChunkExecutionStatus.PENDING)
        .setMaxResults(1)
        .resultList

    if (executions.isNotEmpty()) {
      return executions.first()
    }

    // Fallback: look for any non-completed execution for this job
    val fallbackExecutions =
      entityManager
        .createQuery(
          """
          from BatchJobChunkExecution bjce
          where bjce.batchJob.id = :jobId
          and bjce.status not in :completedStatuses
          order by bjce.id desc
          """.trimIndent(),
          BatchJobChunkExecution::class.java,
        ).setParameter("jobId", job.id)
        .setParameter(
          "completedStatuses",
          listOf(
            BatchJobChunkExecutionStatus.SUCCESS,
            BatchJobChunkExecutionStatus.FAILED,
            BatchJobChunkExecutionStatus.CANCELLED,
          ),
        ).setMaxResults(1)
        .resultList

    return fallbackExecutions.firstOrNull()
      ?: throw IllegalStateException("No active chunk execution found for job ${job.id}")
  }

  override fun getParamsType(): Class<MachineTranslationJobParams> {
    return MachineTranslationJobParams::class.java
  }

  override fun getTarget(data: MachineTranslationRequest): List<BatchTranslationTargetItem> {
    return data.keyIds.flatMap { keyId ->
      data.targetLanguageIds.map { languageId ->
        BatchTranslationTargetItem(keyId, languageId)
      }
    }
  }

  override fun getMaxPerJobConcurrency(): Int {
    return batchProperties.maxPerMtJobConcurrency
  }

  override fun getJobCharacter(): JobCharacter {
    return JobCharacter.SLOW
  }

  override fun getChunkSize(
    request: MachineTranslationRequest,
    projectId: Long?,
  ): Int {
    if (request.useBatchApi) {
      return batchProperties.batchApiChunkSize
    }
    return 5
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getParams(data: MachineTranslationRequest): MachineTranslationJobParams {
    return MachineTranslationJobParams().apply {
      this.targetLanguageIds = data.targetLanguageIds
      this.useBatchApi = data.useBatchApi
    }
  }
}
