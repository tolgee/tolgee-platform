/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.UserStorageResponse
import io.tolgee.hateoas.userPreferences.UserPreferencesModel
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
    organizationRoleService.checkUserCanView(organization.id)
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
}
