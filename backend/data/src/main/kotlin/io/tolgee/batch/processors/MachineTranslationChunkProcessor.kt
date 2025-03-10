package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.constants.MtServiceType
import io.tolgee.model.batch.params.MachineTranslationJobParams
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class MachineTranslationChunkProcessor(
  private val genericAutoTranslationChunkProcessor: GenericAutoTranslationChunkProcessor,
  private val mtServiceConfigService: MtServiceConfigService,
  private val projectService: ProjectService,
) : ChunkProcessor<MachineTranslationRequest, MachineTranslationJobParams, BatchTranslationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    @Suppress("UNCHECKED_CAST")
    genericAutoTranslationChunkProcessor.process(
      job,
      chunk,
      coroutineContext,
      onProgress,
      useMachineTranslation = true,
      useTranslationMemory = false,
      llmPrompt = getParams(job).llmPrompt,
    )
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
    return 1
  }

  override fun getJobCharacter(): JobCharacter {
    return JobCharacter.SLOW
  }

  override fun getChunkSize(
    request: MachineTranslationRequest,
    projectId: Long?,
  ): Int {
    projectId ?: throw IllegalArgumentException("Project id is required")
    val languageIds = request.targetLanguageIds
    val services = mtServiceConfigService.getPrimaryServices(languageIds, projectId).values.toSet()
    if (services.map { it?.serviceType }.contains(MtServiceType.PROMPT)) {
      return 2
    }
    return 5
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getParams(data: MachineTranslationRequest): MachineTranslationJobParams {
    return MachineTranslationJobParams().apply {
      this.targetLanguageIds = data.targetLanguageIds
      this.llmPrompt = data.llmPrompt
    }
  }
}
