package io.tolgee.service.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.service.export.dataProvider.ExportDataProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.project.ProjectService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.trace
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
  private val objectMapper: ObjectMapper,
) : Logging {
  fun export(
    projectId: Long,
    exportParams: IExportParams,
  ): Map<String, InputStream> {
    traceLogExportInfo(exportParams, projectId)
    return traceLogMeasureTime("Export project $projectId data") {
      val data = getDataForExport(projectId, exportParams)
      val baseLanguage = getProjectBaseLanguage(projectId)
      val project = projectService.get(projectId)
      val baseTranslationsProvider =
        getBaseTranslationsProvider(
          exportParams = exportParams,
          projectId = projectId,
          baseLanguage = baseLanguage,
        )

      fileExporterFactory
        .create(
          data = data,
          exportParams = exportParams,
          baseTranslationsProvider = baseTranslationsProvider,
          baseLanguage,
          projectIcuPlaceholdersSupport = project.icuPlaceholders,
        ).produceFiles()
        .also {
          publishBusinessEvent(projectId)
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

  private fun publishBusinessEvent(projectId: Long) {
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

  private fun traceLogExportInfo(
    exportParams: IExportParams,
    projectId: Long,
  ) {
    logger.trace {
      // we don't want to log params which are other then ExportParams, since it can fail on serialization
      val params = exportParams as? ExportParams

      val json =
        try {
          objectMapper.writeValueAsString(params)
        } catch (_: Exception) {
          "[cannot serialize params]"
        }
      "Exporting project $projectId with params $json"
    }
  }
}
