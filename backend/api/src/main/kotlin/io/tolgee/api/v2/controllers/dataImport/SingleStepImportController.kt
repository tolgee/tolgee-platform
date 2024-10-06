/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.Logging
import io.tolgee.util.filterFiles
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/single-step-import", "/v2/projects/single-step-import"])
@ImportDocsTag
class SingleStepImportController(
  private val importService: ImportService,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val securityService: SecurityService,
) : Logging {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(
    summary = "Single step import",
    description =
      "Unlike the /v2/projects/{projectId}/import endpoint, " +
        "imports the data in single request by provided files and parameters. " +
        "This is useful for automated importing via API or CLI.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @RequestBody(
    content =
      [
        Content(
          encoding = [
            Encoding(name = "params", contentType = "application/json"),
          ],
        ),
      ],
  )
  @AllowApiAccess
  @OpenApiOrderExtension(1)
  @RequestActivity(ActivityType.IMPORT)
  fun doImport(
    @RequestPart("files")
    files: Array<MultipartFile>,
    @RequestPart
    @Valid params: SingleStepImportRequest,
  ) {
    val filteredFiles = filterFiles(files.map { (it.originalFilename ?: "") to it })
    val fileDtos =
      filteredFiles.map {
        ImportFileDto(it.originalFilename ?: "", it.inputStream.readAllBytes())
      }

    if (params.removeOtherKeys == true) {
      securityService.checkProjectPermission(projectHolder.project.id, Scope.KEYS_DELETE)
    }

    importService.singleStepImport(
      files = fileDtos,
      project = projectHolder.projectEntity,
      userAccount = authenticationFacade.authenticatedUserEntity,
      params = params,
    ) {}
  }
}
