/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.userPreferences.UserPreferencesModel
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.UserPreferencesService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/user-preferences"])
@Tag(name = "User preferences")
class UserPreferencesController(
  private val userPreferencesService: UserPreferencesService,
  private val authenticationFacade: AuthenticationFacade
) {
  @GetMapping("")
  @Operation(summary = "")
  fun get(): UserPreferencesModel {
    return userPreferencesService.findOrCreate(authenticationFacade.userAccount.id).let {
      UserPreferencesModel(language = it.language, preferredOrganizationId = it.preferredOrganization?.id)
    }
  }

  @PutMapping("/set-language/{languageTag}")
  @Operation(summary = "")
  fun setLanguage(
    @PathVariable languageTag: String
  ) {
    userPreferencesService.setLanguage(languageTag, authenticationFacade.userAccountEntity)
  }
}
