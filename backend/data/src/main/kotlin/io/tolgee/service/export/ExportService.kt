package io.tolgee.service.export

import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.service.export.dataProvider.ExportDataProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.project.ProjectService
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Duration

@Service
class ExportService(
  private val fileExporterFactory: FileExporterFactory,
  private val projectService: ProjectService,
  private val applicationContext: ApplicationContext,
  private val businessEventPublisher: BusinessEventPublisher,
) {
  fun export(
    projectId: Long,
    exportParams: IExportParams,
  ): Map<String, InputStream> {
    val data = getDataForExport(projectId, exportParams)
    val baseLanguage = getProjectBaseLanguage(projectId)
    val project = projectService.get(projectId)
    val baseTranslationsProvider =
      getBaseTranslationsProvider(
        exportParams = exportParams,
        projectId = projectId,
        baseLanguage = baseLanguage,
      )

    return fileExporterFactory.create(
      data = data,
      exportParams = exportParams,
      baseTranslationsProvider = baseTranslationsProvider,
      baseLanguage,
      projectIcuPlaceholdersSupport = project.icuPlaceholders,
    ).produceFiles().also {
      businessEventPublisher.publishOnceInTime(
        OnBusinessEventToCaptureEvent(
          eventName = "EXPORT",
          projectId = projectId,
        ),
        Duration.ofDays(1),
      ) {
        "EXPORT_$projectId"
      }
    }
  }

  /**
   * Base translations are not required for all Formatters.
   * So formatters which need them can call the provider.
   */
  private fun getBaseTranslationsProvider(
    exportParams: IExportParams,
    projectId: Long,
    baseLanguage: LanguageDto,
  ): () -> List<ExportTranslationView> {
    return {
      getDataForExport(projectId, exportParams, listOf(baseLanguage.tag))
    }
  }

  private fun getDataForExport(
    projectId: Long,
    exportParams: IExportParams,
    overrideLanguageTags: List<String>? = null,
  ): List<ExportTranslationView> {
    return ExportDataProvider(
      applicationContext = applicationContext,
      exportParams = exportParams,
      projectId = projectId,
      overrideLanguageTag = overrideLanguageTags,
    ).data
  }

  private fun getProjectBaseLanguage(projectId: Long): LanguageDto {
    return projectService.getOrAssignBaseLanguage(projectId)
  }
}
