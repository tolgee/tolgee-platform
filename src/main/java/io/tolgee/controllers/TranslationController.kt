@file:Suppress("MVCPathVariableInspection")

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.ApiScope
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.request.SetTranslationsDTO
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto
import io.tolgee.dtos.response.ViewDataResponse
import io.tolgee.dtos.response.translations_view.ResponseParams
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.repository_auth.AccessWithAnyRepositoryPermission
import io.tolgee.security.repository_auth.AccessWithRepositoryPermission
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.KeyService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/repository/{repositoryId:[0-9]+}/translations", "/api/repository/translations")
@Tag(name = "Translations", description = "Manipulates localization messages and metadata")
class TranslationController @Autowired constructor(
        private val translationService: TranslationService,
        private val keyService: KeyService,
        private val securityService: SecurityService,
        private val repositoryHolder: RepositoryHolder,
        private val repositoryService: RepositoryService
) : IController {
    @GetMapping(value = ["/{languages}"])
    @AccessWithAnyRepositoryPermission
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
    @Operation(summary = "Get all translations for specific languages")
    fun getTranslations(@PathVariable("languages") languages: Set<String?>?): Map<String, Any> {
        return translationService.getTranslations(languages, repositoryHolder.repository.id)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @PostMapping("/set")
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
    @AccessWithRepositoryPermission(permission = Permission.RepositoryPermissionType.TRANSLATE)
    @Deprecated(message = "Use put method to /api/repository/{repositoryId}/translations or /api/repository/translations")
    @Hidden
    fun setTranslationsPost(@RequestBody @Valid dto: SetTranslationsDTO?) {
        setTranslations(dto)
    }

    @PutMapping("")
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
    @AccessWithRepositoryPermission(permission = Permission.RepositoryPermissionType.TRANSLATE)
    @Operation(summary = "Sets translations for existing key")
    fun setTranslations(@RequestBody @Valid dto: SetTranslationsDTO?) {
        val key = keyService.get(
                repositoryHolder.repository.id,
                PathDTO.fromFullPath(dto!!.key)
        ).orElseThrow { NotFoundException() }

        translationService.setForKey(key, dto.translations!!)
    }

    @PostMapping("")
    @AccessWithApiKey([ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_EDIT])
    @AccessWithRepositoryPermission(permission = Permission.RepositoryPermissionType.EDIT)
    @Operation(summary = "Sets translations for existing or not existing key")
    fun createOrUpdateTranslations(@RequestBody @Valid dto: SetTranslationsDTO) {
        val repository = repositoryService.get(repositoryHolder.repository.id).get()
        val key = keyService.getOrCreateKey(repository, PathDTO.fromFullPath(dto.key))
        translationService.setForKey(key, dto.translations!!)
    }

    @GetMapping(value = ["/view"])
    @Operation(summary = "Returns data for translations view with metadata")
    fun getViewData(@PathVariable("repositoryId") repositoryId: Long?,
                    @RequestParam(name = "languages", required = false) languages: Set<String>?,
                    @RequestParam(name = "limit", defaultValue = "10") limit: Int,
                    @RequestParam(name = "offset", defaultValue = "0") offset: Int,
                    @RequestParam(name = "search", required = false) search: String?
    ): ViewDataResponse<LinkedHashSet<KeyWithTranslationsResponseDto>, ResponseParams> {
        securityService.checkRepositoryPermission(repositoryId!!, Permission.RepositoryPermissionType.VIEW)
        return translationService.getViewData(languages, repositoryId, limit, offset, search)
    }
}
