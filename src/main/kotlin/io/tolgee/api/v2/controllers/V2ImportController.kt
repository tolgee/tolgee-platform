/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.dataImport.ImportLanguageModel
import io.tolgee.api.v2.hateoas.dataImport.ImportLanguageModelAssembler
import io.tolgee.api.v2.hateoas.dataImport.ImportTranslationModel
import io.tolgee.api.v2.hateoas.dataImport.ImportTranslationModelAssembler
import io.tolgee.constants.Message
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.repository_auth.AccessWithRepositoryPermission
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ForceMode
import io.tolgee.service.dataImport.ImportService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.mediatype.hal.HalMediaTypeConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/repositories/{repositoryId}/import"])
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
        private val halMediaTypeConfiguration: HalMediaTypeConfiguration,
        private val repositoryHolder: RepositoryHolder,
        private val languageService: LanguageService
) {

    @PostMapping("/with-streaming-response", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Prepares provided files to import, streams operation progress")
    fun addFilesStreaming(
            @PathVariable("repositoryId") repositoryId: Long,
            @RequestPart("files") files: Array<MultipartFile>,
    ): ResponseEntity<StreamingResponseBody> {
        val stream = StreamingResponseBody { responseStream: OutputStream ->
            val messageClient = { type: ImportStreamingProgressMessageType, params: List<Any>? ->
                responseStream.write(ImportStreamingProgressMessage(type, params).toJsonByteArray())
                responseStream.write(";;;".toByteArray())
                responseStream.flush()
            }
            val fileDtos = files.map { ImportFileDto(it.originalFilename, it.inputStream) }
            importService.addFiles(files = fileDtos, messageClient)
            val result = this.getImportResult(repositoryId, PageRequest.of(0, 100))
            val mapper = jacksonObjectMapper()
            halMediaTypeConfiguration.configureObjectMapper(mapper)
            val jsonByteResult = mapper.writeValueAsBytes(result)
            responseStream.write(jsonByteResult)
        }

        return ResponseEntity(stream, HttpStatus.OK)
    }

    @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Prepares provided files to import, streams operation progress")
    fun addFiles(
            @PathVariable("repositoryId") repositoryId: Long,
            @RequestPart("files") files: Array<MultipartFile>,
    ): PagedModel<ImportLanguageModel> {
        val fileDtos = files.map { ImportFileDto(it.originalFilename, it.inputStream) }
        importService.addFiles(files = fileDtos)
        return this.getImportResult(repositoryId, PageRequest.of(0, 100))
    }

    @PutMapping("/apply")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Imports the data prepared in previous step")
    fun applyImport(
            @PathVariable("repositoryId") repositoryId: Long,
            @Schema(description = "Whether override or keep all translations with unresolved conflicts")
            @RequestParam("forceMode", defaultValue = "NO_FORCE") forceMode: ForceMode,
    ) {
        this.importService.import(repositoryId, authenticationFacade.userAccount.id!!, forceMode)
    }

    @GetMapping("/result")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun getImportResult(
            @PathVariable("repositoryId") repositoryId: Long,
            pageable: Pageable
    ): PagedModel<ImportLanguageModel> {
        val userId = authenticationFacade.userAccount.id!!
        val languages = importService.getResult(repositoryId, userId, pageable)
        return pagedLanguagesResourcesAssembler.toModel(languages, importLanguageModelAssembler)
    }

    @GetMapping("/result/languages/{languageId}/translations")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun getImportTranslations(
            @PathVariable("repositoryId") repositoryId: Long,
            @PathVariable("languageId") languageId: Long,
            @Schema(description = "Whether only translations, which are in conflict " +
                    "with existing translations should be returned")
            @RequestParam("onlyConflicts", defaultValue = "false") onlyConflicts: Boolean = false,
            @Schema(description = "Whether only translations with unresolved conflicts" +
                    "with existing translations should be returned")
            @RequestParam("onlyUnresolved", defaultValue = "false") onlyUnresolved: Boolean = false,
            pageable: Pageable
    ): PagedModel<ImportTranslationModel> {
        checkImportLanguageInRepository(languageId)
        val translations = importService.getTranslations(languageId, pageable, onlyConflicts, onlyUnresolved)
        return pagedTranslationsResourcesAssembler.toModel(translations, importTranslationModelAssembler)
    }

    @DeleteMapping("")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun cancelImport() {
        this.importService.deleteImport(repositoryHolder.repository.id, authenticationFacade.userAccount.id!!)
    }

    @DeleteMapping("/result/languages/{languageId}")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun deleteLanguage(@PathVariable("languageId") languageId: Long) {
        val language = checkImportLanguageInRepository(languageId)
        this.importService.deleteLanguage(language)
    }

    @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-override")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun resolveTranslationSetOverride(
            @PathVariable("languageId") languageId: Long,
            @PathVariable("translationId") translationId: Long
    ) {
        resolveTranslation(languageId, translationId, true)
    }

    @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun resolveTranslationSetKeepExisting(@PathVariable("languageId") languageId: Long,
                                          @PathVariable("translationId") translationId: Long
    ) {
        resolveTranslation(languageId, translationId, false)
    }

    @PutMapping("/result/languages/{languageId}/resolve-all/set-override")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun resolveTranslationSetOverride(
            @PathVariable("languageId") languageId: Long
    ) {
        resolveAllOfLanguage(languageId, true)
    }

    @PutMapping("/result/languages/{languageId}/resolve-all/set-keep-existing")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun resolveTranslationSetKeepExisting(
            @PathVariable("languageId") languageId: Long,
    ) {
        resolveAllOfLanguage(languageId, false)
    }

    @PutMapping("/result/languages/{importLanguageId}/select-existing/{existingLanguageId}")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    fun selectExistingLanguage(
            @PathVariable("importLanguageId") importLanguageId: Long,
            @PathVariable("existingLanguageId") existingLanguageId: Long,
    ) {
        val existingLanguage = checkLanguageFromRepository(existingLanguageId)
        val importLanguage = checkImportLanguageInRepository(importLanguageId)
        this.importService.selectExistingLanguage(importLanguage, existingLanguage)
    }

    private fun resolveAllOfLanguage(languageId: Long, override: Boolean) {
        val language = checkImportLanguageInRepository(languageId)
        importService.resolveAllOfLanguage(language, override)
    }

    private fun resolveTranslation(languageId: Long, translationId: Long, override: Boolean) {
        checkImportLanguageInRepository(languageId)
        val translation = checkTranslationOfLanguage(translationId, languageId)
        return importService.resolveTranslationConflict(translation, override)
    }

    private fun checkLanguageFromRepository(languageId: Long): Language {
        val existingLanguage = languageService.findById(languageId).orElse(null) ?: throw NotFoundException()
        if (existingLanguage.repository!!.id != repositoryHolder.repository.id) {
            throw BadRequestException(Message.IMPORT_LANGUAGE_NOT_FROM_REPOSITORY)
        }
        return existingLanguage
    }

    private fun checkImportLanguageInRepository(languageId: Long): ImportLanguage {
        val language = importService.findLanguage(languageId) ?: throw NotFoundException()
        val languageRepositoryId = language.file.import.repository.id
        if (languageRepositoryId != repositoryHolder.repository.id) {
            throw BadRequestException(Message.IMPORT_LANGUAGE_NOT_FROM_REPOSITORY)
        }
        return language
    }

    private fun checkTranslationOfLanguage(translationId: Long, languageId: Long): ImportTranslation {
        val translation = importService.findTranslation(translationId) ?: throw NotFoundException()

        if (translation.language.id != languageId) {
            throw BadRequestException(Message.IMPORT_LANGUAGE_NOT_FROM_REPOSITORY)
        }
        return translation
    }
}
