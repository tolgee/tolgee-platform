package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AutoTranslationRequest
import io.tolgee.model.batch.params.AutoTranslationJobParams
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.AutoTranslationService
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

@Component
class AutoTranslateChunkProcessor(
  private val genericAutoTranslationChunkProcessor: GenericAutoTranslationChunkProcessor,
  private val mtServiceConfigService: MtServiceConfigService,
  private val autoTranslationService: AutoTranslationService,
  private val projectService: ProjectService,
) : ChunkProcessor<AutoTranslationRequest, AutoTranslationJobParams, BatchTranslationTargetItem> {
  override fun process(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    coroutineContext: CoroutineContext,
    onProgress: (Int) -> Unit,
  ) {
    val projectId = job.projectId ?: throw IllegalArgumentException("Project id is required")
    genericAutoTranslationChunkProcessor.iterateCatching(chunk, coroutineContext) { item ->
      val (keyId, languageId) = item
      autoTranslationService.softAutoTranslate(projectId, keyId, languageId)
    }
  }

  override fun getParamsType(): Class<AutoTranslationJobParams> {
    return AutoTranslationJobParams::class.java
  }

  override fun getTarget(data: AutoTranslationRequest): List<BatchTranslationTargetItem> {
    return data.target
  }

  override fun getMaxPerJobConcurrency(): Int {
    return 1
  }

  override fun getJobCharacter(): JobCharacter {
    return JobCharacter.SLOW
  }

  override fun getChunkSize(
    request: AutoTranslationRequest,
    projectId: Long?,
  ): Int {
    projectId ?: throw IllegalArgumentException("Project id is required")
    val languageIds = request.target.map { it.languageId }.distinct()
    val project = projectService.getDto(projectId)
    val services = mtServiceConfigService.getPrimaryServices(languageIds, project.id).values.toSet()
//    if (!services.mapNotNull { it?.serviceType }.contains(MtServiceType.TOLGEE)) {
//      return 5
//    }
    return 2
  }

  override fun getTargetItemType(): Class<BatchTranslationTargetItem> {
    return BatchTranslationTargetItem::class.java
  }

  override fun getParams(data: AutoTranslationRequest): AutoTranslationJobParams {
    return AutoTranslationJobParams()
  }
}
