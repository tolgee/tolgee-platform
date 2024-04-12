/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.dataImport.ImportSettingsRequest
import io.tolgee.hateoas.dataImport.ImportSettingsModel
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.dataImport.ImportSettingsService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/import-settings", "/v2/projects/import-settings"])
@Tag(
  name = "Import Settings",
  description =
    "These endpoints enable you to store default settings for import. " +
      "These settings are only used in the UI of Tolgee platform. " +
      "It's also the default for stateful importing via `/v2/projects/{projectId}/import/*` API endpoints. " +
      "The settings are stored per user and per project.",
)
class ImportSettingsController(
  private val projectHolder: ProjectHolder,
  private val importSettingsService: ImportSettingsService,
  private val authenticationFacade: AuthenticationFacade,
) {
  @GetMapping("")
  @Operation(
    summary = "Get Import Settings",
    description = "Returns import settings for the authenticated user and the project.",
  )
  @AllowApiAccess
  @UseDefaultPermissions
  fun get(): ImportSettingsModel {
    val projectId = projectHolder.project.id
    val settings = importSettingsService.get(authenticationFacade.authenticatedUserEntity, projectId)
    return ImportSettingsModel(settings)
  }

  @PutMapping("")
  @Operation(
    summary = "Set Import Settings",
    description = "Stores import settings for the authenticated user and the project.",
  )
  @AllowApiAccess
  @UseDefaultPermissions
  fun store(
    @Valid @RequestBody dto: ImportSettingsRequest,
  ): ImportSettingsModel {
    val projectId = projectHolder.project.id
    val settings = importSettingsService.store(authenticationFacade.authenticatedUserEntity, projectId, dto)
    return ImportSettingsModel(settings)
  }
}
