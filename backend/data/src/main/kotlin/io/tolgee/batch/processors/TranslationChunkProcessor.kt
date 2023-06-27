package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.FailedDontRequeueException
import io.tolgee.batch.RequeueWithTimeoutException
import io.tolgee.batch.request.BatchTranslateRequest
import io.tolgee.constants.Message
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.TranslateJobParams
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.AutoTranslationService
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import kotlin.coroutines.CoroutineContext

@Component
class TranslationChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val entityManager: EntityManager
) : ChunkProcessor<BatchTranslateRequest> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit
  ) {
    val keys = keyService.find(chunk)
    val parameters = getParams(job)
    val languages = languageService.findByIdIn(parameters.targetLanguageIds)

    val successfulTargets = mutableListOf<Long>()
    keys.forEach { key ->
      coroutineContext.ensureActive()
      try {
        autoTranslationService.autoTranslate(
          key = key,
          languageTags = languages.map { it.tag },
          useTranslationMemory = parameters.useTranslationMemory,
          useMachineTranslation = parameters.useMachineTranslation
        )
        successfulTargets.add(key.id)
      } catch (e: OutOfCreditsException) {
        throw FailedDontRequeueException(Message.OUT_OF_CREDITS, successfulTargets, e)
      } catch (e: Throwable) {
        throw RequeueWithTimeoutException(Message.TRANSLATION_FAILED, successfulTargets, e)
      }
    }
  }

  private fun getParams(job: BatchJobDto): TranslateJobParams {
    return entityManager.createQuery("""from TranslateJobParams tjp where tjp.batchJob.id = :batchJobId""")
      .setParameter("batchJobId", job.id).singleResult as? TranslateJobParams
      ?: throw IllegalStateException("No params found")
  }

  override fun getTarget(data: BatchTranslateRequest): List<Long> {
    return data.keyIds
  }

  override fun getParams(request: BatchTranslateRequest, job: BatchJob): EntityWithId {
    return TranslateJobParams().apply {
      this.batchJob = job
      this.targetLanguageIds = request.targetLanguageIds
      this.useMachineTranslation = request.useMachineTranslation
      this.useTranslationMemory = request.useTranslationMemory
      this.service = request.service
    }
  }
}
