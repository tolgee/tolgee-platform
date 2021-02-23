package io.tolgee.controllers

import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.request.UaaGetKeyTranslations
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AllowAccessWithApiKey
import io.tolgee.service.*
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/uaa")
class UserAppApiController(
        private val translationService: TranslationService,
        private val keyService: KeyService,
        private val repositoryService: RepositoryService,
        private val securityService: SecurityService,
        private val authenticationFacade: AuthenticationFacade,
        private val languageService: LanguageService,
) {
    @GetMapping(value = ["/{languages}"])
    @AllowAccessWithApiKey
    fun getTranslations(@PathVariable("languages") languages: Set<String?>?): Map<String, Any> {
        val apiKey = authenticationFacade.apiKey
        securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
        return translationService.getTranslations(languages, apiKey.repository!!.id)
    }

    @GetMapping(value = ["/source/{key:.+}/{languages}", "/key/{key:.+}/{languages}"])
    @AllowAccessWithApiKey
    @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
    fun getKeyTranslations(@PathVariable("key") fullPath: String?,
                           @PathVariable("languages") langs: Set<String?>?): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(fullPath)
        val apiKey = authenticationFacade.apiKey
        securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, langs)
    }

    @PostMapping(value = ["/keyTranslations/{languages}"])
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    fun getKeyTranslationsPost(@RequestBody body: UaaGetKeyTranslations, @PathVariable("languages") langs: Set<String?>?): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(body.key)
        val apiKey = authenticationFacade.apiKey
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, langs)
    }

    @GetMapping(value = ["/source/{key:.+}"])
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
    fun getKeyTranslations(@PathVariable("key") fullPath: String?): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(fullPath)
        val apiKey = authenticationFacade.apiKey
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, null)
    }

    @PostMapping(value = ["/keyTranslations"])
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    fun getKeyTranslationsPost(@RequestBody body: UaaGetKeyTranslations): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(body.key)
        val apiKey = authenticationFacade.apiKey
        return translationService.getKeyTranslationsResult(apiKey.repository!!.id, pathDTO, null)
    }

    @PostMapping("")
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
    fun setTranslations(@RequestBody dto: @Valid SetTranslationsDTO) {
        val apiKey = authenticationFacade.apiKey
        val repository = repositoryService.findById(apiKey.repository!!.id).orElseThrow { NotFoundException(Message.REPOSITORY_NOT_FOUND) }
        val key = keyService.getOrCreateKey(repository, PathDTO.fromFullPath(dto.key))
        translationService.setForKey(key, dto.translations)
    }


    @Deprecated(message = "Use /api/languages")
    @get:AllowAccessWithApiKey
    @get:GetMapping("/languages")
    val languages: Set<String>
        get() {
            val apiKey = authenticationFacade.apiKey
            securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_EDIT), apiKey)
            return languageService.findAll(apiKey.repository!!.id).stream().map { obj: Language -> obj.abbreviation!! }.collect(Collectors.toSet())
        }

    @get:AllowAccessWithApiKey
    @get:GetMapping("/scopes")
    val scopes: Set<String>
        get() {
            val apiKey = authenticationFacade.apiKey
            return apiKey.getScopesSet().stream().map { obj: ApiScope -> obj.value }.collect(Collectors.toSet())
        }
}