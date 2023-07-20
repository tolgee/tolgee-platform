package io.tolgee.service.export

import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.constants.Message
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.service.export.dataProvider.ExportDataProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Duration
import javax.persistence.EntityManager

@Service
class ExportService(
  private val fileExporterFactory: FileExporterFactory,
  private val projectService: ProjectService,
  private val entityManager: EntityManager,
  private val businessEventPublisher: BusinessEventPublisher
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
    ).produceFiles().also {
      businessEventPublisher.publishOnceInTime(
        OnBusinessEventToCaptureEvent(
          eventName = "EXPORT",
          projectId = projectId
        ),
        Duration.ofDays(1)
      )
    }
  }

  /**
   * Base translations are not required for all Formatters.
   * So formatters which need them can call the provider.
   */
  private fun getBaseTranslationsProvider(
    exportParams: ExportParams,
    projectId: Long,
    baseLanguage: Language
  ): () -> List<ExportTranslationView> {
    return {
      getDataForExport(projectId, exportParams.copy(languages = setOf(baseLanguage.tag), filterState = null))
    }
  }

  private fun getDataForExport(projectId: Long, exportParams: ExportParams): List<ExportTranslationView> {
    return ExportDataProvider(
      entityManager = entityManager,
      exportParams = exportParams,
      projectId = projectId
    ).getData()
  }

  private fun getProjectBaseLanguage(projectId: Long): Language {
    return projectService.getOrCreateBaseLanguage(projectId)
      ?: throw NotFoundException(Message.CANNOT_FIND_BASE_LANGUAGE)
  }
}
