/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.dataImport.ImportService
import io.tolgee.util.Logging
import io.tolgee.util.filterFiles
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/single-step-import", "/v2/projects/single-step-import"])
@Tag(
  name = "Import",
  description = "These endpoints handle multi-step data import",
)
class SingleStepImportController(
  private val importService: ImportService,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val streamingImportProgressUtil: StreamingImportProgressUtil,
) : Logging {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(description = "Prepares provided files to import.", summary = "Add files")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun doImport(
    @RequestPart("files") files: Array<MultipartFile>,
    @Parameter(schema = Schema(type = "string", format = "binary"))
    @RequestPart("params")
    @Valid params: SingleStepImportRequest,
  ): ResponseEntity<StreamingResponseBody> {
    val filteredFiles = filterFiles(files.map { (it.originalFilename ?: "") to it })
    val fileDtos =
      filteredFiles.map {
        ImportFileDto(it.originalFilename ?: "", it.inputStream.readAllBytes())
      }

    return streamingImportProgressUtil.stream { writeStatus ->

      importService.singleStepImport(
        files = fileDtos,
        project = projectHolder.projectEntity,
        userAccount = authenticationFacade.authenticatedUserEntity,
        params = params,
        reportStatus = writeStatus,
      )
    }
  }
}
