package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.GetKeyTranslationsReqDto
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.service.*
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/uaa")
@Deprecated(message = "This endpoint is deprecated. Integration libraries should use standard endpoints for each resource.")
@Hidden
class UserAppApiController(
        private val translationService: TranslationService,
        private val keyService: KeyService,
        private val repositoryService: RepositoryService,
        private val securityService: SecurityService,
        private val authenticationFacade: AuthenticationFacade,
        private val languageService: LanguageService,
) {
    @GetMapping(value = ["/{languages}"])
    @AccessWithApiKey
    @Deprecated(message = "Use standard /api/repository/translations/...")
    fun getTranslations(@PathVariable("languages") languages: Set<String?>?): Map<String, Any> {
        val apiKey = authenticationFacade.apiKey
        securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
        return translationService.getTranslations(languages, apiKey.repository!!.id)
    }

    @GetMapping(value = ["/source/{key:.+}/{languages}", "/key/{key:.+}/{languages}"])
    @AccessWithApiKey
    @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
    fun getKeyTranslations(@PathVariable("key") fullPath: String?,
                           @PathVariable("languages") langs: Set<String>?): Map<String, String?> {
        val pathDTO = PathDTO.fromFullPath(fullPath)
        val apiKey = authenticationFacade.apiKey
        securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, langs)
    }

    @PostMapping(value = ["/keyTranslations/{languages}"])
    @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    fun getKeyTranslationsPost(@RequestBody body: GetKeyTranslationsReqDto, @PathVariable("languages") langs: Set<String>?): Map<String, String?> {
        val pathDTO = PathDTO.fromFullPath(body.key)
        val apiKey = authenticationFacade.apiKey
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, langs)
    }

    @GetMapping(value = ["/source/{key:.+}"])
    @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
    fun getKeyTranslations(@PathVariable("key") fullPath: String?): Map<String, String?> {
        val pathDTO = PathDTO.fromFullPath(fullPath)
        val apiKey = authenticationFacade.apiKey
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, null)
    }

    @PostMapping(value = ["/keyTranslations"])
    @AccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    fun getKeyTranslationsPost(@RequestBody body: GetKeyTranslationsReqDto): Map<String, String?> {
        val pathDTO = PathDTO.fromFullPath(body.key)
        val apiKey = authenticationFacade.apiKey
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, null)
    }

    @PostMapping("")
    @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
    fun setTranslations(@RequestBody @Valid dto: SetTranslationsDTO) {
        val apiKey = authenticationFacade.apiKey
        val repository = repositoryService.get(apiKey.repository!!.id).orElseThrow { NotFoundException(Message.REPOSITORY_NOT_FOUND) }!!
        val key = keyService.getOrCreateKey(repository, PathDTO.fromFullPath(dto.key))
        translationService.setForKey(key, dto.translations!!)
    }

    @Deprecated(message = "Use /api/repository/languages")
    @AccessWithApiKey
    @GetMapping("/languages")
    fun getLanguages(): Set<String> {
        val apiKey = authenticationFacade.apiKey
        securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_EDIT), apiKey)
        return languageService.findAll(apiKey.repository!!.id).stream().map { obj: Language -> obj.abbreviation!! }.collect(Collectors.toSet())
    }

    @AccessWithApiKey
    @GetMapping("/scopes")
    fun getScopes(): Set<String> {
        val apiKey = authenticationFacade.apiKey
        return apiKey.scopesEnum.asSequence().map { it.value }.toSet()
    }
}
