package io.tolgee.batch.processors

import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.model.batch.params.MachineTranslationJobParams
import io.tolgee.service.LanguageService
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class MachineTranslationChunkProcessor(
  private val languageService: LanguageService,
  private val genericAutoTranslationChunkProcessor: GenericAutoTranslationChunkProcessor
) : ChunkProcessor<MachineTranslationRequest, MachineTranslationJobParams> {
  override fun process(
    job: BatchJobDto,
    chunk: List<Long>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit
  ) {
    val parameters = getParams(job)
    val languages = languageService.findByIdIn(parameters.targetLanguageIds)

    genericAutoTranslationChunkProcessor.process(
      job,
      chunk,
      coroutineContext,
      onProgress,
      GenericAutoTranslationChunkProcessor.Type.MACHINE_TRANSLATION,
      languages
    )
  }

  override fun getParamsType(): Class<MachineTranslationJobParams> {
    return MachineTranslationJobParams::class.java
  }

  override fun getTarget(data: MachineTranslationRequest): List<Long> {
    return data.keyIds
  }

  override fun getParams(data: MachineTranslationRequest): MachineTranslationJobParams {
    return MachineTranslationJobParams().apply {
      this.targetLanguageIds = data.targetLanguageIds
    }
  }
}
