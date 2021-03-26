package io.tolgee.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.ImportDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.service.ImportService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import javax.validation.Valid


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/repository/{repositoryId}/import")
@Tag(name = "Import")
class ImportController(
        private val repositoryService: RepositoryService,
        private val securityService: SecurityService,
        private val importService: ImportService
) : IController {

    @PostMapping(value = [""])
    fun doImport(@PathVariable("repositoryId")
                 repositoryId: Long,
                 @RequestBody @Valid
                 dto: ImportDto
    ): ResponseEntity<StreamingResponseBody> {
        securityService.checkRepositoryPermission(repositoryId, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.getById(repositoryId).orElseThrow {
            NotFoundException(Message.REPOSITORY_NOT_FOUND)
        }

        val stream = StreamingResponseBody { out: OutputStream ->
            importService.import(repository, dto, out)
        }

        return ResponseEntity(stream, HttpStatus.OK)
    }
}
