/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.model.Permission
import io.tolgee.security.repository_auth.AccessWithRepositoryPermission
import io.tolgee.service.import.ImportService
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
open class V2ImportController(
        private val importService: ImportService
) {

    @PostMapping("")
    @AccessWithRepositoryPermission(Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Prepares provided files to import, streams operation progress")
    open fun import(
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

}
