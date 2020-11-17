package io.polygloat.controllers

import io.polygloat.constants.ApiScope
import io.polygloat.constants.Message
import io.polygloat.dtos.PathDTO
import io.polygloat.dtos.request.SetTranslationsDTO
import io.polygloat.dtos.request.UaaGetKeyTranslations
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.Language
import io.polygloat.security.AuthenticationFacade
import io.polygloat.security.api_key_auth.AllowAccessWithApiKey
import io.polygloat.service.*
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/uaa")
class UserAppApiController(
        private val translationService: TranslationService,
        private val sourceService: SourceService,
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
        return translationService.getTranslations(languages, apiKey.repository.id)
    }

    @GetMapping(value = ["/source/{key:.+}/{languages}"])
    @AllowAccessWithApiKey
    @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
    fun getSourceTranslations(@PathVariable("key") fullPath: String?,
                              @PathVariable("languages") langs: Set<String?>?): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(fullPath)
        val apiKey = authenticationFacade.apiKey
        securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_VIEW), apiKey)
        return translationService.getSourceTranslationsResult(apiKey.repository.id, pathDTO, langs)
    }

    @PostMapping(value = ["/keyTranslations/{languages}"])
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    fun getKeyTranslationsPost(@RequestBody body: UaaGetKeyTranslations, @PathVariable("languages") langs: Set<String?>?): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(body.key)
        val apiKey = authenticationFacade.apiKey
        return translationService.getSourceTranslationsResult(apiKey.repository.id, pathDTO, langs)
    }

    @GetMapping(value = ["/source/{key:.+}"])
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    @Deprecated("can not pass . as parameter of text, for longer texts it would be much better to use POST")
    fun getSourceTranslations(@PathVariable("key") fullPath: String?): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(fullPath)
        val apiKey = authenticationFacade.apiKey
        return translationService.getSourceTranslationsResult(apiKey.repository.id, pathDTO, null)
    }

    @PostMapping(value = ["/keyTranslations"])
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_VIEW])
    fun getKeyTranslationsPost(@RequestBody body: UaaGetKeyTranslations): Map<String, String> {
        val pathDTO = PathDTO.fromFullPath(body.key)
        val apiKey = authenticationFacade.apiKey
        return translationService.getSourceTranslationsResult(apiKey.repository.id, pathDTO, null)
    }

    @PostMapping("")
    @AllowAccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
    fun setTranslations(@RequestBody dto: @Valid SetTranslationsDTO) {
        val apiKey = authenticationFacade.apiKey
        val repository = repositoryService.findById(apiKey.repository.id).orElseThrow { NotFoundException(Message.REPOSITORY_NOT_FOUND) }
        val source = sourceService.getOrCreateSource(repository, PathDTO.fromFullPath(dto.key))
        translationService.setForSource(source, dto.translations)
    }


    @Deprecated(message = "Use /api/languages")
    @get:GetMapping("/languages")
    val languages: Set<String>
        @AllowAccessWithApiKey
        get() {
            val apiKey = authenticationFacade.apiKey
            securityService.checkApiKeyScopes(setOf(ApiScope.TRANSLATIONS_EDIT), apiKey)
            return languageService.findAll(apiKey.repository.id).stream().map { obj: Language -> obj.abbreviation }.collect(Collectors.toSet())
        }

    @AllowAccessWithApiKey
    @get:GetMapping("/scopes")
    val scopes: Set<String>
        get() {
            val apiKey = authenticationFacade.apiKey
            return apiKey.scopes.stream().map { obj: ApiScope -> obj.value }.collect(Collectors.toSet())
        }
}