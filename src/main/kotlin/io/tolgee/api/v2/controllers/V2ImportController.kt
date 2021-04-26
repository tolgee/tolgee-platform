/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
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
import io.tolgee.service.dataImport.ImportService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

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
) {

    @PostMapping("")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Prepares provided files to import, streams operation progress")
    fun import(
            @PathVariable("repositoryId") repositoryId: Long,
            @RequestParam("files") files: Array<MultipartFile>,
    ): ResponseEntity<StreamingResponseBody> {
        val stream = StreamingResponseBody { responseStream: OutputStream ->
            val messageClient = { type: ImportStreamingProgressMessageType, params: List<Any>? ->
                responseStream.write(ImportStreamingProgressMessage(type, params).toJsonByteArray())
                responseStream.write(";;;".toByteArray())
            }
            val fileDtos = files.map { ImportFileDto(it.originalFilename, it.inputStream) }
            importService.doImport(files = fileDtos, messageClient)
        }

        return ResponseEntity(stream, HttpStatus.OK)
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
        //security check
        val languageRepositoryId = importService.findLanguage(languageId)?.file?.import?.repository?.id
        if (languageRepositoryId != repositoryId) {
            throw BadRequestException(Message.IMPORT_LANGUAGE_NOT_FROM_REPOSITORY)
        }

        val translations = importService.getTranslations(languageId, pageable, onlyCollisions)
        return pagedTranslationsResourcesAssembler.toModel(translations, importTranslationModelAssembler)
    }
}
