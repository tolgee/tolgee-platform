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
import io.tolgee.model.Permission
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.repository_auth.AccessWithRepositoryPermission
import io.tolgee.security.repository_auth.RepositoryHolder
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
        private val repositoryHolder: RepositoryHolder
) {

    @PostMapping("/with-streaming-response", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Prepares provided files to import, streams operation progress")
    fun importStreaming(
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
            importService.doImport(files = fileDtos, messageClient)
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
    fun import(
            @PathVariable("repositoryId") repositoryId: Long,
            @RequestPart("files") files: Array<MultipartFile>,
    ): PagedModel<ImportLanguageModel> {
        val fileDtos = files.map { ImportFileDto(it.originalFilename, it.inputStream) }
        importService.doImport(files = fileDtos)
        return this.getImportResult(repositoryId, PageRequest.of(0, 100))
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
            @RequestParam("onlyCollisions", defaultValue = "true") onlyCollisions: Boolean = true,
            pageable: Pageable
    ): PagedModel<ImportTranslationModel> {
        checkUserOwnsLanguage(languageId)
        val translations = importService.getTranslations(languageId, pageable, onlyCollisions)
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
        checkUserOwnsLanguage(languageId)
        this.importService.deleteLanguage(languageId)
    }

    private fun checkUserOwnsLanguage(languageId: Long) {
        val languageRepositoryId = importService.findLanguage(languageId)?.file?.import?.repository?.id
        if (languageRepositoryId != repositoryHolder.repository.id) {
            throw BadRequestException(Message.IMPORT_LANGUAGE_NOT_FROM_REPOSITORY)
        }
    }
}
