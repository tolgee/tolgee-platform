package io.tolgee.service.export

import io.tolgee.constants.Message
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.dtos.request.export.ExportParamsNull
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.ProjectService
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ExportService(
  private val translationRepository: TranslationRepository,
  private val fileExporterFactory: FileExporterFactory,
  private val projectService: ProjectService
) {
  fun export(projectId: Long, exportParams: ExportParams): Map<String, InputStream> {
    val data = getDataForExport(projectId, exportParams)
    val baseLanguage = getProjectBaseLanguage(projectId)
    val baseTranslationsProvider = getBaseTranslationsProvider(
      exportParams = exportParams,
      projectId = projectId,
      baseLanguage = baseLanguage
    )
    return fileExporterFactory.create(
      data = data,
      exportParams = exportParams,
      baseTranslationsProvider = baseTranslationsProvider,
      baseLanguage
    ).produceFiles()
  }

  /**
   * Base translations are not required for all Formatters.
   * So formatters which need them can call the provider.
   */
  private fun getBaseTranslationsProvider(
    exportParams: ExportParams,
    projectId: Long,
    baseLanguage: Language
  ): () -> List<Translation> {
    return {
      getDataForExport(projectId, exportParams.copy(languages = setOf(baseLanguage.tag)))
    }
  }

  private fun getDataForExport(projectId: Long, exportParams: ExportParams): List<Translation> {
    return translationRepository.getDataForExport(
      projectId = projectId,
      params = exportParams,
      pnn = ExportParamsNull(exportParams)
    )
  }

  private fun getProjectBaseLanguage(projectId: Long): Language {
    return projectService.autoSetBaseLanguage(projectId)
      ?: throw NotFoundException(Message.CANNOT_FIND_BASE_LANGUAGE)
  }
}
