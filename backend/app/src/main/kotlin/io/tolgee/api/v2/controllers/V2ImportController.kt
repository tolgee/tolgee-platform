/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.ActivityType
import io.tolgee.activity.RequestActivity
import io.tolgee.api.v2.hateoas.dataImport.ImportAddFilesResultModel
import io.tolgee.api.v2.hateoas.dataImport.ImportLanguageModel
import io.tolgee.api.v2.hateoas.dataImport.ImportLanguageModelAssembler
import io.tolgee.api.v2.hateoas.dataImport.ImportTranslationModel
import io.tolgee.api.v2.hateoas.dataImport.ImportTranslationModelAssembler
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ForceMode
import io.tolgee.service.dataImport.ImportService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.mediatype.hal.HalMediaTypeConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId}/import"])
@Tag(name = "Import")
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

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val halMediaTypeConfiguration: HalMediaTypeConfiguration,
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
) {

  @PostMapping("/with-streaming-response", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @RequestBody
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Prepares provided files to import, streams operation progress")
  fun addFilesStreaming(
    @PathVariable("projectId") projectId: Long,
    @RequestPart("files") files: Array<MultipartFile>,
  ): ResponseEntity<StreamingResponseBody> {
    val stream = StreamingResponseBody { responseStream: OutputStream ->
      val messageClient = { type: ImportStreamingProgressMessageType, params: List<Any>? ->
        responseStream.write(ImportStreamingProgressMessage(type, params).toJsonByteArray())
        responseStream.write(";;;".toByteArray())
        responseStream.flush()
      }
      val fileDtos = files.map { ImportFileDto(it.originalFilename ?: "", it.inputStream) }
      val errors = importService.addFiles(
        files = fileDtos,
        messageClient = messageClient,
        project = projectHolder.projectEntity,
        userAccount = authenticationFacade.userAccountEntity
      )

      val result = getImportAddFilesResultModel(projectId, errors)

      val mapper = jacksonObjectMapper()
      halMediaTypeConfiguration.configureObjectMapper(mapper)
      val jsonByteResult = mapper.writeValueAsBytes(result)

      responseStream.write(jsonByteResult)
    }

    return ResponseEntity(stream, HttpStatus.OK)
  }

  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Prepares provided files to import")
  fun addFiles(
    @PathVariable("projectId") projectId: Long,
    @RequestPart("files") files: Array<MultipartFile>,
  ): ImportAddFilesResultModel {
    val fileDtos = files.map { ImportFileDto(it.originalFilename ?: "", it.inputStream) }
    val errors = importService.addFiles(
      files = fileDtos,
      project = projectHolder.projectEntity,
      userAccount = authenticationFacade.userAccountEntity
    )
    return getImportAddFilesResultModel(projectId, errors)
  }

  private fun getImportAddFilesResultModel(
    projectId: Long,
    errors: List<ErrorResponseBody>
  ): ImportAddFilesResultModel {
    val result: PagedModel<ImportLanguageModel>? = try {
      this.getImportResult(projectId, PageRequest.of(0, 100))
    } catch (e: NotFoundException) {
      null
    }
    return ImportAddFilesResultModel(errors, result)
  }

  @PutMapping("/apply")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @Operation(summary = "Imports the data prepared in previous step")
  @RequestActivity(ActivityType.IMPORT)
  fun applyImport(
    @PathVariable("projectId") projectId: Long,
    @Schema(description = "Whether override or keep all translations with unresolved conflicts")
    @RequestParam("forceMode", defaultValue = "NO_FORCE") forceMode: ForceMode,
  ) {
    this.importService.import(projectId, authenticationFacade.userAccount.id, forceMode)
  }

  @GetMapping("/result")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun getImportResult(
    @PathVariable("projectId") projectId: Long,
    @ParameterObject pageable: Pageable
  ): PagedModel<ImportLanguageModel> {
    val userId = authenticationFacade.userAccount.id
    val languages = importService.getResult(projectId, userId, pageable)
    return pagedLanguagesResourcesAssembler.toModel(languages, importLanguageModelAssembler)
  }

  @GetMapping("/result/languages/{languageId}")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun getImportLanguage(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("projectId") projectId: Long,
  ): ImportLanguageModel {
    checkImportLanguageInProject(languageId)
    val language = importService.findLanguageView(languageId) ?: throw NotFoundException()
    return importLanguageModelAssembler.toModel(language)
  }

  @GetMapping("/result/languages/{languageId}/translations")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun getImportTranslations(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("languageId") languageId: Long,
    @Schema(
      description = "Whether only translations, which are in conflict " +
        "with existing translations should be returned"
    )
    @RequestParam("onlyConflicts", defaultValue = "false") onlyConflicts: Boolean = false,
    @Schema(
      description = "Whether only translations with unresolved conflicts" +
        "with existing translations should be returned"
    )
    @RequestParam("onlyUnresolved", defaultValue = "false") onlyUnresolved: Boolean = false,
    @Schema(description = "String to search in translation text or key")
    @RequestParam("search") search: String? = null,
    @ParameterObject @SortDefault("keyName") pageable: Pageable
  ): PagedModel<ImportTranslationModel> {
    checkImportLanguageInProject(languageId)
    val translations = importService.getTranslationsView(languageId, pageable, onlyConflicts, onlyUnresolved, search)
    return pagedTranslationsResourcesAssembler.toModel(translations, importTranslationModelAssembler)
  }

  @DeleteMapping("")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun cancelImport() {
    this.importService.deleteImport(projectHolder.project.id, authenticationFacade.userAccount.id)
  }

  @DeleteMapping("/result/languages/{languageId}")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun deleteLanguage(@PathVariable("languageId") languageId: Long) {
    val language = checkImportLanguageInProject(languageId)
    this.importService.deleteLanguage(language)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-override")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long
  ) {
    resolveTranslation(languageId, translationId, true)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long
  ) {
    resolveTranslation(languageId, translationId, false)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-override")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long
  ) {
    resolveAllOfLanguage(languageId, true)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-keep-existing")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
  ) {
    resolveAllOfLanguage(languageId, false)
  }

  @PutMapping("/result/languages/{importLanguageId}/select-existing/{existingLanguageId}")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun selectExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
    @PathVariable("existingLanguageId") existingLanguageId: Long,
  ) {
    val existingLanguage = checkLanguageFromProject(existingLanguageId)
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, existingLanguage)
  }

  @PutMapping("/result/languages/{importLanguageId}/reset-existing")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun resetExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
  ) {
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, null)
  }

  @GetMapping("/result/files/{importFileId}/issues")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  fun getImportFileIssues(
    @PathVariable("importFileId") importFileId: Long,
    @ParameterObject pageable: Pageable
  ): PagedModel<EntityModel<ImportFileIssueView>> {
    checkFileFromProject(importFileId)
    val page = importService.getFileIssues(importFileId, pageable)
    return pagedImportFileIssueResourcesAssembler.toModel(page)
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
    if (existingLanguage.project!!.id != projectHolder.project.id) {
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
