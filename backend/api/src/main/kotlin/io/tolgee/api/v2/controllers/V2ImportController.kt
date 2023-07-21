/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

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
import io.tolgee.hateoas.dataImport.ImportLanguageModel
import io.tolgee.hateoas.dataImport.ImportLanguageModelAssembler
import io.tolgee.hateoas.dataImport.ImportNamespaceModel
import io.tolgee.hateoas.dataImport.ImportTranslationModel
import io.tolgee.hateoas.dataImport.ImportTranslationModelAssembler
import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ForceMode
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.security.SecurityService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.http.MediaType
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
import javax.servlet.http.HttpServletRequest

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/import", "/v2/projects/import"])
@Tag(
  name = "Import",
  description = "These endpoints handle multi-step data import"
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
  private val securityService: SecurityService
) {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Prepares provided files to import.", summary = "Add files")
  fun addFiles(
    @RequestPart("files") files: Array<MultipartFile>,
    @ParameterObject params: ImportAddFilesParams
  ): ImportAddFilesResultModel {
    checkBaseImportPermissions()
    val fileDtos = files.map { ImportFileDto(it.originalFilename ?: "", it.inputStream) }
    val errors = importService.addFiles(
      files = fileDtos,
      project = projectHolder.projectEntity,
      userAccount = authenticationFacade.userAccountEntity,
      params = params
    )
    return getImportAddFilesResultModel(errors)
  }

  private fun getImportAddFilesResultModel(
    errors: List<ErrorResponseBody>
  ): ImportAddFilesResultModel {
    val result: PagedModel<ImportLanguageModel>? = try {
      this.getImportResult(PageRequest.of(0, 100))
    } catch (e: NotFoundException) {
      null
    }
    return ImportAddFilesResultModel(errors, result)
  }

  @PutMapping("/apply")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Imports the data prepared in previous step", summary = "Apply")
  @RequestActivity(ActivityType.IMPORT)
  fun applyImport(
    @Parameter(description = "Whether override or keep all translations with unresolved conflicts")
    @RequestParam(defaultValue = "NO_FORCE")
    forceMode: ForceMode,
  ) {
    val projectId = projectHolder.project.id
    checkBaseImportPermissions()
    this.importService.import(projectId, authenticationFacade.userAccount.id, forceMode)
  }

  private fun checkBaseImportPermissions() {
    securityService.checkProjectPermission(projectHolder.project.id, Scope.TRANSLATIONS_VIEW)
  }

  @GetMapping("/result")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Returns the result of preparation.", summary = "Get result")
  fun getImportResult(
    @ParameterObject pageable: Pageable
  ): PagedModel<ImportLanguageModel> {
    checkBaseImportPermissions()
    val projectId = projectHolder.project.id
    val userId = authenticationFacade.userAccount.id
    val languages = importService.getResult(projectId, userId, pageable)
    return pagedLanguagesResourcesAssembler.toModel(languages, importLanguageModelAssembler)
  }

  @GetMapping("/result/languages/{languageId}")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Returns language prepared to import.", summary = "Get import language")
  fun getImportLanguage(
    @PathVariable("languageId") languageId: Long,
  ): ImportLanguageModel {
    checkBaseImportPermissions()
    checkImportLanguageInProject(languageId)
    val language = importService.findLanguageView(languageId) ?: throw NotFoundException()
    return importLanguageModelAssembler.toModel(language)
  }

  @GetMapping("/result/languages/{languageId}/translations")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Returns translations prepared to import.", summary = "Get translations")
  fun getImportTranslations(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("languageId") languageId: Long,
    @Parameter(
      description = "Whether only translations, which are in conflict " +
        "with existing translations should be returned"
    )
    @RequestParam("onlyConflicts", defaultValue = "false") onlyConflicts: Boolean = false,
    @Parameter(
      description = "Whether only translations with unresolved conflicts" +
        "with existing translations should be returned"
    )
    @RequestParam("onlyUnresolved", defaultValue = "false") onlyUnresolved: Boolean = false,
    @Parameter(description = "String to search in translation text or key")
    @RequestParam("search") search: String? = null,
    @ParameterObject @SortDefault("keyName") pageable: Pageable
  ): PagedModel<ImportTranslationModel> {
    checkBaseImportPermissions()
    checkImportLanguageInProject(languageId)
    val translations = importService.getTranslationsView(languageId, pageable, onlyConflicts, onlyUnresolved, search)
    return pagedTranslationsResourcesAssembler.toModel(translations, importTranslationModelAssembler)
  }

  @DeleteMapping("")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Deletes prepared import data.", summary = "Delete")
  fun cancelImport() {
    checkBaseImportPermissions()
    this.importService.deleteImport(projectHolder.project.id, authenticationFacade.userAccount.id)
  }

  @DeleteMapping("/result/languages/{languageId}")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(description = "Deletes language prepared to import.", summary = "Delete language")
  fun deleteLanguage(@PathVariable("languageId") languageId: Long) {
    checkBaseImportPermissions()
    val language = checkImportLanguageInProject(languageId)
    this.importService.deleteLanguage(language)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-override")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Resolves translation conflict. The old translation will be overridden.",
    summary = "Resolve conflict (override)"
  )
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long
  ) {
    checkBaseImportPermissions()
    resolveTranslation(languageId, translationId, true)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Resolves translation conflict. The old translation will be kept.",
    summary = "Resolve conflict (keep existing)"
  )
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long
  ) {
    checkBaseImportPermissions()
    resolveTranslation(languageId, translationId, false)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-override")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Resolves all translation conflicts for provided language. The old translations will be overridden.",
    summary = "Resolve all translation conflicts (override)"
  )
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long
  ) {
    checkBaseImportPermissions()
    resolveAllOfLanguage(languageId, true)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-keep-existing")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Resolves all translation conflicts for provided language. The old translations will be kept.",
    summary = "Resolve all translation conflicts (keep existing)"
  )
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
  ) {
    checkBaseImportPermissions()
    resolveAllOfLanguage(languageId, false)
  }

  @PutMapping("/result/files/{fileId}/select-namespace")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Sets namespace for file to import.",
    summary = "Select namespace"
  )
  fun selectNamespace(
    @PathVariable fileId: Long,
    @RequestBody req: SetFileNamespaceRequest,
    request: HttpServletRequest
  ) {
    checkBaseImportPermissions()
    val file = checkFileFromProject(fileId)
    this.importService.selectNamespace(file, req.namespace)
  }

  @PutMapping("/result/languages/{importLanguageId}/select-existing/{existingLanguageId}")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Sets existing language to pair with language to import. " +
      "Data will be imported to selected existing language when applied.",
    summary = "Pair existing language"
  )
  fun selectExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
    @PathVariable("existingLanguageId") existingLanguageId: Long,
  ) {
    checkBaseImportPermissions()
    val existingLanguage = checkLanguageFromProject(existingLanguageId)
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, existingLanguage)
  }

  @PutMapping("/result/languages/{importLanguageId}/reset-existing")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Resets existing language paired with language to import.",
    summary = "Reset existing language pairing"
  )
  fun resetExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
  ) {
    checkBaseImportPermissions()
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, null)
  }

  @GetMapping("/result/files/{importFileId}/issues")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Returns issues for uploaded file.",
    summary = "Get file issues"
  )
  fun getImportFileIssues(
    @PathVariable("importFileId") importFileId: Long,
    @ParameterObject pageable: Pageable
  ): PagedModel<EntityModel<ImportFileIssueView>> {
    checkFileFromProject(importFileId)
    val page = importService.getFileIssues(importFileId, pageable)
    return pagedImportFileIssueResourcesAssembler.toModel(page)
  }

  @GetMapping("/all-namespaces")
  @AccessWithAnyProjectPermission()
  @AccessWithApiKey()
  @Operation(
    description = "Returns all existing and imported namespaces",
    summary = "Get namespaces"
  )
  fun getAllNamespaces(): CollectionModel<ImportNamespaceModel> {
    checkBaseImportPermissions()
    val import = importService.get(
      projectId = projectHolder.project.id,
      authorId = authenticationFacade.userAccount.id
    )
    val importNamespaces = importService.getAllNamespaces(import.id)
    val existingNamespaces = namespaceService.getAllInProject(projectId = projectHolder.project.id)
    val result = existingNamespaces
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
    result: MutableMap<String, ImportNamespaceModel>
  ): CollectionModel<ImportNamespaceModel> {
    val assembler = object : RepresentationModelAssemblerSupport<ImportNamespaceModel, ImportNamespaceModel>(
      this::class.java,
      ImportNamespaceModel::class.java
    ) {
      override fun toModel(entity: ImportNamespaceModel): ImportNamespaceModel = entity
    }

    return assembler.toCollectionModel(result.values.sortedBy { it.name })
  }

  private fun resolveAllOfLanguage(languageId: Long, override: Boolean) {
    val language = checkImportLanguageInProject(languageId)
    importService.resolveAllOfLanguage(language, override)
  }

  private fun resolveTranslation(languageId: Long, translationId: Long, override: Boolean) {
    checkImportLanguageInProject(languageId)
    val translation = checkTranslationOfLanguage(translationId, languageId)
    return importService.resolveTranslationConflict(translation, override)
  }

  private fun checkFileFromProject(fileId: Long): ImportFile {
    val file = importService.findFile(fileId) ?: throw NotFoundException()
    if (file.import.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return file
  }

  private fun checkLanguageFromProject(languageId: Long): Language {
    val existingLanguage = languageService.findById(languageId).orElse(null) ?: throw NotFoundException()
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

  private fun checkTranslationOfLanguage(translationId: Long, languageId: Long): ImportTranslation {
    val translation = importService.findTranslation(translationId) ?: throw NotFoundException()

    if (translation.language.id != languageId) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return translation
  }
}
