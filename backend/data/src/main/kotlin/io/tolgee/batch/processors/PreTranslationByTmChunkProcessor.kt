package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.PreTranslationByTmRequest
import io.tolgee.model.batch.params.PreTranslationByTmJobParams
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class PreTranslationByTmChunkProcessor(
  private val genericAutoTranslationChunkProcessor: GenericAutoTranslationChunkProcessor,
) : ChunkProcessor<PreTranslationByTmRequest, PreTranslationByTmJobParams, BatchTranslationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
  ) {
    genericAutoTranslationChunkProcessor.process(
      job,
      chunk,
      coroutineContext,
      useTranslationMemory = true,
      useMachineTranslation = false,
    )
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getTarget(data: PreTranslationByTmRequest): List<BatchTranslationTargetItem> {
    return data.keyIds.flatMap { keyId ->
      data.targetLanguageIds.map { languageId ->
        BatchTranslationTargetItem(keyId, languageId)
      }
    }
  }

  override fun getParamsType(): Class<PreTranslationByTmJobParams> {
    return PreTranslationByTmJobParams::class.java
  }

  override fun getChunkSize(
    request: PreTranslationByTmRequest,
    projectId: Long?,
  ): Int {
    return 10
  }

  override fun getParams(data: PreTranslationByTmRequest): PreTranslationByTmJobParams {
    return PreTranslationByTmJobParams().apply {
      this.targetLanguageIds = data.targetLanguageIds
    }
  }
}
