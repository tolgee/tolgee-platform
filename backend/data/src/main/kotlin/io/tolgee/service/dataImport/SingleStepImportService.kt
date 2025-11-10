package io.tolgee.service.dataImport

import io.sentry.Sentry
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.ImportResult
import io.tolgee.dtos.SimpleKeyResult
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.KeyDefinitionDto
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.dtos.request.importKeysResolvable.ResolvableTranslationResolution
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.service.dataImport.ScreenshotImporter.Companion.ScreenshotToImport
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable

@Service
@Transactional
class SingleStepImportService(
  @Lazy
  private val self: SingleStepImportService,
  private val importService: ImportService,
  private val currentDateProvider: CurrentDateProvider,
  private val applicationContext: ApplicationContext,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun singleStepImport(
    files: List<ImportFileDto>,
    project: Project,
    userAccount: UserAccount,
    params: SingleStepImportRequest,
    reportStatus: ((ImportApplicationStatus) -> Unit) = {},
    screenshots: List<ScreenshotToImport> = emptyList(),
    resolveConflict: ((translation: ImportTranslation) -> ForceMode?)? = null,
  ): ImportResult {
    reportStatus?.invoke(ImportApplicationStatus.ANALYZING_FILES)
    val import = Import(project).also { it.author = userAccount }

    importService.publishImportBusinessEvent(project.id, userAccount.id)

    val importId = "single-step-p${project.id}-a${userAccount.id}-${currentDateProvider.date.time}"
    Sentry.addBreadcrumb("Import ID: $importId")
    importService.saveFilesToFileStorage(importId, files, params.storeFilesToFileStorage)

    val fileProcessor =
      CoreImportFilesProcessor(
        applicationContext = applicationContext,
        // for single step import the import entity is passed only as a holder for the import data like
        // author, project, file issues, etc
        import = import,
        params = params,
        projectIcuPlaceholdersEnabled = project.icuPlaceholders,
        importSettings = params,
        saveData = false,
      )
    fileProcessor.processFiles(files)

    if (fileProcessor.errors.isNotEmpty()) {
      @Suppress("UNCHECKED_CAST")
      throw BadRequestException(Message.IMPORT_FAILED, fileProcessor.errors as List<Serializable>)
    }

    if (fileProcessor.importDataManager.storedLanguages.isEmpty() && screenshots.isEmpty()) {
      throw BadRequestException(Message.NO_DATA_TO_IMPORT)
    }

    entityManager.clear()

    val result =
      StoredDataImporter(
        applicationContext,
        import,
        params.forceMode,
        reportStatus,
        importSettings = params,
        _importDataManager = fileProcessor.importDataManager,
        isSingleStepImport = true,
        overrideMode = params.overrideMode ?: OverrideMode.RECOMMENDED,
        errorOnUnresolvedConflict = params.errorOnUnresolvedConflict,
        resolveConflict = resolveConflict,
        screenshots = screenshots,
      ).doImport()

    return result
  }

  @Transactional
  fun singleStepImportResolvable(
    project: Project,
    userAccount: UserAccount,
    params: SingleStepImportResolvableRequest,
    reportStatus: ((ImportApplicationStatus) -> Unit) = {},
  ): ImportResult {
    val keysToFilesManager = KeysToFilesManager()
    keysToFilesManager.processKeys(params.keys)

    val request = SingleStepImportRequest()
    request.overrideMode = params.overrideMode ?: OverrideMode.RECOMMENDED
    request.errorOnUnresolvedConflict = params.errorOnUnresolvedConflict

    // these options are not user accessible,
    // because it might act weird when just importing screenshots without any actual translations
    // leaving this for an actual usecase as it's now not clear how it should behave
    request.convertPlaceholdersToIcu = false
    request.tagNewKeys = emptyList()
    request.fileMappings = keysToFilesManager.getFileMappings()
    request.removeOtherKeys = false
    request.createNewKeys = true

    val conflictResolutionMap = keysToFilesManager.getConflictResolutionMap()

    val screenshots: List<ScreenshotToImport> =
      params.keys.flatMap { key ->
        key.screenshots?.map { sc ->
          ScreenshotToImport(
            key = KeyDefinitionDto(key.name, key.namespace),
            screenshot = sc,
          )
        } ?: emptyList()
      }

    return self.singleStepImport(
      files = keysToFilesManager.getDtos(),
      project = project,
      userAccount,
      request,
      reportStatus,
      screenshots = screenshots,
      resolveConflict = { translation ->
        val resolution =
          conflictResolutionMap
            .get(translation.key.file.namespace)
            ?.get(translation.language.name)
            ?.get(translation.key.name)

        when (resolution) {
          null -> ForceMode.OVERRIDE
          ResolvableTranslationResolution.OVERRIDE -> ForceMode.OVERRIDE
          ResolvableTranslationResolution.EXPECT_NO_CONFLICT -> {
            if (translation.text != translation.conflict?.text) {
              throw BadRequestException(Message.EXPECT_NO_CONFLICT_FAILED, getConflictingKeys(translation))
            } else {
              ForceMode.KEEP
            }
          }
        }
      },
    )
  }

  fun getConflictingKeys(importKey: ImportTranslation): List<SimpleKeyResult> {
    return importKey.conflict?.let {
      listOf(SimpleKeyResult(it.id, it.key.name, it.key.namespace?.name))
    } ?: emptyList()
  }
}
