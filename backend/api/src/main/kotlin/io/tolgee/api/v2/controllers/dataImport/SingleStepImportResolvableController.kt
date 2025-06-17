/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.ImportResult
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableRequest
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.Logging
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
  "/v2/projects/{projectId:\\d+}/single-step-import-resolvable",
  "/v2/projects/single-step-import-resolvable"
]
)
@ImportDocsTag
class SingleStepImportResolvableController(
  private val importService: ImportService,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val securityService: SecurityService,
) : Logging {
  @PostMapping("")
  @Operation(
    summary = "Single step import from body",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @OpenApiOrderExtension(1)
  @RequestActivity(ActivityType.IMPORT)
  fun doImport(
    @RequestBody @Valid params: SingleStepImportResolvableRequest,
  ): ImportResult {
    if (params.removeOtherKeys == true) {
      securityService.checkProjectPermission(projectHolder.project.id, Scope.KEYS_DELETE)
    }

    return importService.singleStepImportResolvable(
      project = projectHolder.projectEntity,
      userAccount = authenticationFacade.authenticatedUserEntity,
      params = params,
    )
  }
}
