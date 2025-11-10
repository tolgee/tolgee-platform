/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.keys

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.controllers.IController
import io.tolgee.dtos.request.translation.SelectAllResponse
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@KeysDocsTag
@RequestMapping(value = [""])
class SelectAllController(
  private val projectHolder: ProjectHolder,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  @GetMapping(
    value = [
      "/v2/projects/{projectId:[0-9]+}/translations/select-all",
      "/v2/projects/translations/select-all",
      "/v2/projects/{projectId:[0-9]+}/keys/select",
      "/v2/projects/keys/select",
    ],
  )
  @Operation(
    summary = "Select keys",
    description =
      "Returns all key IDs for specified filter values. " +
        "This way, you can apply the same filter as in the translation view and get the " +
        "resulting key IDs for future use.",
  )
  @RequiresProjectPermissions([Scope.KEYS_VIEW])
  @AllowApiAccess
  @OpenApiHideFromPublicDocs(
    paths = [
      // should be included in keys, not in translations
      "/v2/projects/translations/select-all",
      "/v2/projects/{projectId:[0-9]+}/translations/select-all",
    ],
  )
  fun selectKeys(
    @ParameterObject
    @ModelAttribute("translationFilters")
    params: TranslationFilters,
  ): SelectAllResponse {
    val languages =
      languageService.getLanguagesForTranslationsView(
        params.languages,
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
      )

    return SelectAllResponse(
      translationService.getSelectAllKeys(
        projectId = projectHolder.project.id,
        params = params,
        languages = languages,
      ),
    )
  }
}
