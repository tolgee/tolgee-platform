/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.SetFileNamespaceRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.dataImport.ImportAddFilesResultModel
import io.tolgee.hateoas.dataImport.ImportFileIssueModel
import io.tolgee.hateoas.dataImport.ImportFileIssueModelAssembler
import io.tolgee.hateoas.dataImport.ImportLanguageModel
import io.tolgee.hateoas.dataImport.ImportLanguageModelAssembler
import io.tolgee.hateoas.dataImport.ImportNamespaceModel
import io.tolgee.hateoas.dataImport.ImportTranslationModel
import io.tolgee.hateoas.dataImport.ImportTranslationModelAssembler
import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ForceMode
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.key.NamespaceService
import io.tolgee.util.Logging
import io.tolgee.util.StreamingResponseBodyProvider
import io.tolgee.util.filterFiles
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/import", "/v2/projects/import"])
@Tag(
  name = "Import",
  description = "These endpoints handle multi-step data import",
)
class V2ImportController(
  private val importService: ImportService,
  private val authenticationFacade: AuthenticationFacade,
  private val importLanguageModelAssembler: ImportLanguageModelAssembler,
  private val importTranslationModelAssembler: ImportTranslationModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedLanguagesResourcesAssembler: PagedResourcesAssembler<ImportLanguageView>,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedTranslationsResourcesAssembler: PagedResourcesAssembler<ImportTranslationView>,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedImportFileIssueResourcesAssembler: PagedResourcesAssembler<ImportFileIssueView>,
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val namespaceService: NamespaceService,
  private val importFileIssueModelAssembler: ImportFileIssueModelAssembler,
  private val streamingResponseBodyProvider: StreamingResponseBodyProvider,
  private val streamingImportProgressUtil: StreamingImportProgressUtil,
) : Logging {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(description = "Prepares provided files to import.", summary = "Add files")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun addFiles(
    @RequestPart("files") files: Array<MultipartFile>,
    @ParameterObject params: ImportAddFilesParams,
  ): ImportAddFilesResultModel {
    val filteredFiles = filterFiles(files.map { (it.originalFilename ?: "") to it })
    val fileDtos =
      filteredFiles.map {
        ImportFileDto(it.originalFilename ?: "", it.inputStream.readAllBytes())
      }
    val errors =
      importService.addFiles(
        files = fileDtos,
        project = projectHolder.projectEntity,
        userAccount = authenticationFacade.authenticatedUserEntity,
        params = params,
      )
    return getImportAddFilesResultModel(errors)
  }

  private fun getImportAddFilesResultModel(errors: List<ErrorResponseBody>): ImportAddFilesResultModel {
    val result: PagedModel<ImportLanguageModel>? =
      try {
        this.getImportResult(PageRequest.of(0, 100))
      } catch (e: NotFoundException) {
        null
      }
    return ImportAddFilesResultModel(errors, result)
  }

  @PutMapping("/apply")
  @Operation(description = "Imports the data prepared in previous step")
  @RequestActivity(ActivityType.IMPORT)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun applyImport(
    @Parameter(description = "Whether override or keep all translations with unresolved conflicts")
    @RequestParam(defaultValue = "NO_FORCE")
    forceMode: ForceMode,
  ) {
    val projectId = projectHolder.project.id
    this.importService.import(projectId, authenticationFacade.authenticatedUser.id, forceMode)
  }

  @PutMapping("/apply-streaming", produces = [MediaType.APPLICATION_NDJSON_VALUE])
  @Operation(description = "Imports the data prepared in previous step. Streams current status.")
  @RequestActivity(ActivityType.IMPORT)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun applyImportStreaming(
    @Parameter(description = "Whether override or keep all translations with unresolved conflicts")
    @RequestParam(defaultValue = "NO_FORCE")
    forceMode: ForceMode,
  ): ResponseEntity<StreamingResponseBody> {
    val projectId = projectHolder.project.id

    return streamingImportProgressUtil.stream { writeStatus ->
      this.importService.import(projectId, authenticationFacade.authenticatedUser.id, forceMode, writeStatus)
    }
  }

  @GetMapping("/result")
  @Operation(description = "Returns the result of preparation.", summary = "Get result")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportResult(
    @ParameterObject pageable: Pageable,
  ): PagedModel<ImportLanguageModel> {
    val projectId = projectHolder.project.id
    val userId = authenticationFacade.authenticatedUser.id
    val languages = importService.getResult(projectId, userId, pageable)
    return pagedLanguagesResourcesAssembler.toModel(languages, importLanguageModelAssembler)
  }

  @GetMapping("/result/languages/{languageId}")
  @Operation(description = "Returns language prepared to import.", summary = "Get import language")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportLanguage(
    @PathVariable("languageId") languageId: Long,
  ): ImportLanguageModel {
    checkImportLanguageInProject(languageId)
    val language = importService.findLanguageView(languageId) ?: throw NotFoundException()
    return importLanguageModelAssembler.toModel(language)
  }

  @GetMapping("/result/languages/{languageId}/translations")
  @Operation(description = "Returns translations prepared to import.", summary = "Get translations")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportTranslations(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("languageId") languageId: Long,
    @Parameter(
      description =
        "Whether only translations, which are in conflict " +
          "with existing translations should be returned",
    )
    @RequestParam("onlyConflicts", defaultValue = "false")
    onlyConflicts: Boolean = false,
    @Parameter(
      description =
        "Whether only translations with unresolved conflicts" +
          "with existing translations should be returned",
    )
    @RequestParam("onlyUnresolved", defaultValue = "false")
    onlyUnresolved: Boolean = false,
    @Parameter(description = "String to search in translation text or key")
    @RequestParam("search")
    search: String? = null,
    @ParameterObject
    @SortDefault("keyName")
    pageable: Pageable,
  ): PagedModel<ImportTranslationModel> {
    checkImportLanguageInProject(languageId)
    val translations =
      importService.getTranslationsView(
        languageId,
        pageable,
        onlyConflicts,
        onlyUnresolved,
        search,
      )
    return pagedTranslationsResourcesAssembler.toModel(translations, importTranslationModelAssembler)
  }

  @DeleteMapping("")
  @Operation(description = "Deletes prepared import data.", summary = "Delete")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun cancelImport() {
    this.importService.deleteImport(projectHolder.project.id, authenticationFacade.authenticatedUser.id)
  }

  @DeleteMapping("/result/languages/{languageId}")
  @Operation(description = "Deletes language prepared to import.", summary = "Delete language")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun deleteLanguage(
    @PathVariable("languageId") languageId: Long,
  ) {
    val language = checkImportLanguageInProject(languageId)
    this.importService.deleteLanguage(language)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-override")
  @Operation(
    description = "Resolves translation conflict. The old translation will be overridden.",
    summary = "Resolve conflict (override)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long,
  ) {
    resolveTranslation(languageId, translationId, true)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing")
  @Operation(
    description = "Resolves translation conflict. The old translation will be kept.",
    summary = "Resolve conflict (keep existing)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long,
  ) {
    resolveTranslation(languageId, translationId, false)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-override")
  @Operation(
    description = "Resolves all translation conflicts for provided language. The old translations will be overridden.",
    summary = "Resolve all translation conflicts (override)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long,
  ) {
    resolveAllOfLanguage(languageId, true)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-keep-existing")
  @Operation(
    description = "Resolves all translation conflicts for provided language. The old translations will be kept.",
    summary = "Resolve all translation conflicts (keep existing)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
  ) {
    resolveAllOfLanguage(languageId, false)
  }

  @PutMapping("/result/files/{fileId}/select-namespace")
  @Operation(
    description = "Sets namespace for file to import.",
    summary = "Select namespace",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun selectNamespace(
    @PathVariable fileId: Long,
    @RequestBody req: SetFileNamespaceRequest,
  ) {
    val file = checkFileFromProject(fileId)
    this.importService.selectNamespace(file, req.namespace)
  }

  @PutMapping("/result/languages/{importLanguageId}/select-existing/{existingLanguageId}")
  @Operation(
    description =
      "Sets existing language to pair with language to import. " +
        "Data will be imported to selected existing language when applied.",
    summary = "Pair existing language",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun selectExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
    @PathVariable("existingLanguageId") existingLanguageId: Long,
  ) {
    val existingLanguage = checkLanguageFromProject(existingLanguageId)
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, existingLanguage)
  }

  @PutMapping("/result/languages/{importLanguageId}/reset-existing")
  @Operation(
    description = "Resets existing language paired with language to import.",
    summary = "Reset existing language pairing",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resetExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
  ) {
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, null)
  }

  @GetMapping("/result/files/{importFileId}/issues")
  @Operation(
    description = "Returns issues for uploaded file.",
    summary = "Get file issues",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportFileIssues(
    @PathVariable("importFileId") importFileId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<ImportFileIssueModel> {
    checkFileFromProject(importFileId)
    val page = importService.getFileIssues(importFileId, pageable)
    return pagedImportFileIssueResourcesAssembler.toModel(page, importFileIssueModelAssembler)
  }

  @GetMapping("/all-namespaces")
  @Operation(
    description = "Returns all existing and imported namespaces",
    summary = "Get namespaces",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getAllNamespaces(): CollectionModel<ImportNamespaceModel> {
    val import =
      importService.get(
        projectId = projectHolder.project.id,
        authorId = authenticationFacade.authenticatedUser.id,
      )
    val importNamespaces = importService.getAllNamespaces(import.id)
    val existingNamespaces = namespaceService.getAllInProject(projectId = projectHolder.project.id)
    val result =
      existingNamespaces
        .map { it.name to ImportNamespaceModel(it.id, it.name) }
        .toMap(mutableMapOf())
    importNamespaces.filterNotNull().forEach { importNamespace ->
      result.computeIfAbsent(importNamespace) {
        ImportNamespaceModel(id = null, name = importNamespace)
      }
    }

    return getNamespacesCollectionModel(result)
  }

  private fun getNamespacesCollectionModel(
    result: MutableMap<String, ImportNamespaceModel>,
  ): CollectionModel<ImportNamespaceModel> {
    val assembler =
      object : RepresentationModelAssemblerSupport<ImportNamespaceModel, ImportNamespaceModel>(
        this::class.java,
        ImportNamespaceModel::class.java,
      ) {
        override fun toModel(entity: ImportNamespaceModel): ImportNamespaceModel = entity
      }

    return assembler.toCollectionModel(result.values.sortedBy { it.name })
  }

  private fun resolveAllOfLanguage(
    languageId: Long,
    override: Boolean,
  ) {
    val language = checkImportLanguageInProject(languageId)
    importService.resolveAllOfLanguage(language, override)
  }

  private fun resolveTranslation(
    languageId: Long,
    translationId: Long,
    override: Boolean,
  ) {
    checkImportLanguageInProject(languageId)
    return importService.resolveTranslationConflict(translationId, languageId, override)
  }

  private fun checkFileFromProject(fileId: Long): ImportFile {
    val file = importService.findFile(fileId) ?: throw NotFoundException()
    if (file.import.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return file
  }

  private fun checkLanguageFromProject(languageId: Long): Language {
    val existingLanguage = languageService.getEntity(languageId)
    if (existingLanguage.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return existingLanguage
  }

  private fun checkImportLanguageInProject(languageId: Long): ImportLanguage {
    val language = importService.findLanguage(languageId) ?: throw NotFoundException()
    val languageProjectId = language.file.import.project.id
    if (languageProjectId != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return language
  }
}
