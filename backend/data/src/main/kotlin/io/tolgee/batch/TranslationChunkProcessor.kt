package io.tolgee.batch

import io.tolgee.constants.Message
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.EntityWithId
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.TranslateJobParams
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class TranslationChunkProcessor(
  private val autoTranslationService: AutoTranslationService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val entityManager: EntityManager
) : ChunkProcessor {
  override fun process(job: BatchJob, chunk: List<Long>) {
    val keys = keyService.find(chunk)
    val parameters = getParams(job)
    val languages = languageService.findByIdIn(parameters.targetLanguageIds)

    val successfulTargets = mutableListOf<Long>()
    keys.forEach { key ->
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

  private fun getParams(job: BatchJob): TranslateJobParams {
    return entityManager.createQuery("""from TranslateJobParams tjp where tjp.batchJob.id = :batchJobId""")
      .setParameter("batchJobId", job.id).singleResult as? TranslateJobParams
      ?: throw IllegalStateException("No params found")
  }

  override fun getTarget(data: Any): List<Long> {
    return (data as BatchTranslateRequest).keyIds
  }

  override fun getParams(request: Any, job: BatchJob): EntityWithId {
    val data = (request as BatchTranslateRequest)
    return TranslateJobParams().apply {
      this.batchJob = job
      this.targetLanguageIds = data.targetLanguageIds
      this.useMachineTranslation = data.useMachineTranslation
      this.useTranslationMemory = data.useTranslationMemory
      this.service = data.service
    }
  }
}
