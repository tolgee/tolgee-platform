package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.translation.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/uaa")
@Deprecated(
  message = "This endpoint is deprecated." +
    " Integration libraries should use standard endpoints for each resource."
)
@Hidden
class UserAppApiController(
  private val translationService: TranslationService,
  private val keyService: KeyService,
  private val projectService: ProjectService,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val languageService: LanguageService,
) {
  @GetMapping(value = ["/{languages}"])
  @AccessWithApiKey
  @Deprecated(message = "Use standard /api/project/translations/...")
  fun getTranslations(@PathVariable("languages") languages: Set<String>): Map<String, Any> {
    val apiKey = authenticationFacade.apiKey
    securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
    return translationService.getTranslations(languages, apiKey.project.id)
  }

  @GetMapping(value = ["/source/{key:.+}/{languages}", "/key/{key:.+}/{languages}"])
  @AccessWithApiKey
  @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
  fun getKeyTranslations(
    @PathVariable("key") fullPath: String?,
    @PathVariable("languages") langs: Set<String>?
  ): Map<String, String?> {
    val pathDTO = PathDTO.fromFullPath(fullPath)
    val apiKey = authenticationFacade.apiKey
    securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
    return translationService.getKeyTranslationsResult(apiKey.project.id, pathDTO, langs)
  }

  @PostMapping(value = ["/keyTranslations/{languages}"])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  fun getKeyTranslationsPost(
    @RequestBody body: GetKeyTranslationsReqDto,
    @PathVariable("languages") langs: Set<String>?
  ): Map<String, String?> {
    val pathDTO = PathDTO.fromFullPath(body.key)
    val apiKey = authenticationFacade.apiKey
    return translationService.getKeyTranslationsResult(apiKey.project.id, pathDTO, langs)
  }

  @GetMapping(value = ["/source/{key:.+}"])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
  fun getKeyTranslations(@PathVariable("key") fullPath: String?): Map<String, String?> {
    val pathDTO = PathDTO.fromFullPath(fullPath)
    val apiKey = authenticationFacade.apiKey
    return translationService.getKeyTranslationsResult(apiKey.project.id, pathDTO, null)
  }

  @PostMapping(value = ["/keyTranslations"])
  @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
  fun getKeyTranslationsPost(@RequestBody body: GetKeyTranslationsReqDto): Map<String, String?> {
    val pathDTO = PathDTO.fromFullPath(body.key)
    val apiKey = authenticationFacade.apiKey
    return translationService.getKeyTranslationsResult(apiKey.project.id, pathDTO, null)
  }

  @PostMapping("")
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  fun setTranslations(@RequestBody @Valid dto: SetTranslationsWithKeyDto) {
    val apiKey = authenticationFacade.apiKey
    val project = projectService.get(apiKey.project.id)
    val key = keyService.getOrCreateKey(project, PathDTO.fromFullPath(dto.key))
    translationService.setForKey(key, dto.translations)
  }

  @Deprecated(message = "Use /api/project/languages")
  @AccessWithApiKey
  @GetMapping("/languages")
  fun getLanguages(): Set<String> {
    val apiKey = authenticationFacade.apiKey
    securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_EDIT), apiKey)
    return languageService.findAll(apiKey.project.id).map { obj -> obj.tag }.toSet()
  }

  @AccessWithApiKey
  @GetMapping("/scopes")
  fun getScopes(): Set<String> {
    val apiKey = authenticationFacade.apiKey
    return apiKey.scopesEnum.asSequence().map { it.value }.toSet()
  }
}
