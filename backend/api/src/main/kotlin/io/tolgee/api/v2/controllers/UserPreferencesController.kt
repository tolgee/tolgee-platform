/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.UserStorageResponse
import io.tolgee.hateoas.userPreferences.UserPreferencesModel
import io.tolgee.security.ProjectContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserPreferencesService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/user-preferences"])
@Tag(name = "User preferences")
class UserPreferencesController(
  private val userPreferencesService: UserPreferencesService,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val organizationService: OrganizationService,
  private val projectContextService: ProjectContextService,
) {
  @GetMapping("")
  @Operation(summary = "Get user's preferences")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun get(): UserPreferencesModel {
    return userPreferencesService.findOrCreate(authenticationFacade.authenticatedUser.id).let {
      UserPreferencesModel(language = it.language, preferredOrganizationId = it.preferredOrganization?.id)
    }
  }

  @PutMapping("/set-language/{languageTag}")
  @Operation(summary = "Set user's UI language")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun setLanguage(
    @PathVariable languageTag: String,
  ) {
    userPreferencesService.setLanguage(languageTag, authenticationFacade.authenticatedUserEntity)
  }

  @PutMapping("/set-preferred-organization/{organizationId}")
  @Operation(summary = "Set user preferred organization")
  fun setPreferredOrganization(
    @PathVariable organizationId: Long,
  ) {
    val organization = organizationService.get(organizationId)
    organizationRoleService.checkUserCanViewOrPublic(organization.id)
    userPreferencesService.setPreferredOrganization(organization, authenticationFacade.authenticatedUserEntity)
  }

  @GetMapping("/storage/{fieldName}")
  @Operation(summary = "Get specific field from user's storage")
  fun getStorageField(
    @PathVariable fieldName: String,
  ): UserStorageResponse {
    val preferences = userPreferencesService.findOrCreate(authenticationFacade.authenticatedUser.id)
    val storage = preferences.storageJson ?: emptyMap()
    return UserStorageResponse(storage[fieldName])
  }

  @PutMapping("/storage/{fieldName}")
  @Operation(summary = "Set specific field in user storage")
  fun setStorageField(
    @PathVariable fieldName: String,
    @RequestBody data: Any?,
  ) {
    userPreferencesService.setStorageJsonField(
      fieldName,
      data,
      authenticationFacade.authenticatedUserEntity,
    )
  }

  @GetMapping("/project-storage/{projectId}/{fieldName}")
  @Operation(
    summary = "Get specific field from user's project storage",
    description =
      "Returns a field the current user stored for the given project, e.g. the last used export settings. " +
        "Data is `null` when the field is not set. Requires any access to the project.",
  )
  fun getProjectStorageField(
    @PathVariable projectId: Long,
    @PathVariable fieldName: String,
  ): UserStorageResponse {
    checkCanViewProject(projectId)
    val data =
      userPreferencesService.getProjectStorageField(
        authenticationFacade.authenticatedUser.id,
        projectId,
        fieldName,
      )
    return UserStorageResponse(data)
  }

  @PutMapping("/project-storage/{projectId}/{fieldName}")
  @Operation(
    summary = "Set specific field in user's project storage",
    description =
      "Stores any JSON value under the field for the current user and the given project. " +
        "Sending `null` removes the field. Other fields and other projects are left untouched. " +
        "Field name must match `[A-Za-z0-9_.-]{1,64}`, the serialized value must not exceed 16 KB " +
        "and a project can hold at most 50 fields. Requires any access to the project.",
  )
  fun setProjectStorageField(
    @PathVariable projectId: Long,
    @PathVariable fieldName: String,
    @RequestBody data: Any?,
  ) {
    checkCanViewProject(projectId)
    userPreferencesService.setProjectStorageField(
      authenticationFacade.authenticatedUser.id,
      projectId,
      fieldName,
      data,
    )
  }

  private fun checkCanViewProject(projectId: Long) {
    projectContextService.setup(
      projectId,
      requiredScopes = null,
      useDefaultPermissions = true,
      isWriteOperation = false,
    )
  }
}
