package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.dtos.request.CreateApiKeyDTO
import io.tolgee.dtos.request.EditApiKeyDTO
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.service.ApiKeyService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/apiKeys")
@Tag(name = "API keys")
class ApiKeyController(private val apiKeyService: ApiKeyService,
                       private val repositoryService: RepositoryService,
                       private val authenticationFacade: AuthenticationFacade,
                       private val securityService: SecurityService
) {
    @Operation(summary = "Returns all user's api keys")
    @GetMapping(path = [""])
    fun allByUser(): Set<ApiKeyDTO> {
        return apiKeyService.getAllByUser(authenticationFacade.userAccount).stream()
                .map { apiKey: ApiKey? -> ApiKeyDTO.fromEntity(apiKey) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @GetMapping(path = ["/repository/{repositoryId}"])
    @Operation(summary = "Returns all API keys for repository")
    fun allByRepository(@PathVariable("repositoryId") repositoryId: Long?): Set<ApiKeyDTO> {
        securityService.checkRepositoryPermission(repositoryId!!, ProjectPermissionType.MANAGE)
        return apiKeyService.getAllByRepository(repositoryId).stream()
                .map { apiKey: ApiKey? -> ApiKeyDTO.fromEntity(apiKey) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @PostMapping(path = [""])
    @Operation(summary = "Creates new API key with provided scopes")
    fun create(@RequestBody @Valid createApiKeyDTO: CreateApiKeyDTO?): ApiKeyDTO {
        val repository = repositoryService.get(createApiKeyDTO!!.repositoryId).orElseThrow { NotFoundException(Message.REPOSITORY_NOT_FOUND) }
        securityService.checkApiKeyScopes(createApiKeyDTO.scopes, repository)
        return apiKeyService.createApiKey(authenticationFacade.userAccount, createApiKeyDTO.scopes, repository)
    }

    @PostMapping(path = ["/edit"])
    @Operation(summary = "Edits existing API key")
    fun edit(@RequestBody @Valid dto: EditApiKeyDTO?) {
        val apiKey = apiKeyService.getApiKey(dto!!.id).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
        securityService.checkApiKeyScopes(dto.scopes, apiKey.project)
        apiKey.scopesEnum = dto.scopes
        apiKeyService.editApiKey(apiKey)
    }

    @DeleteMapping(path = ["/{key}"])
    @Operation(summary = "Deletes API key")
    fun delete(@PathVariable("key") key: String?) {
        val apiKey = apiKeyService.getApiKey(key).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
        try {
            securityService.checkRepositoryPermission(apiKey.project!!.id, ProjectPermissionType.MANAGE)
        } catch (e: PermissionException) {
            //user can delete their own api keys
            if (apiKey.userAccount!!.id != authenticationFacade.userAccount.id) {
                throw e
            }
        }
        apiKeyService.deleteApiKey(apiKey)
    }

    @GetMapping(path = ["/availableScopes"])
    @Operation(summary = "Returns API key scopes for every permission type")
    fun getScopes(): Map<String, Set<String>> = Arrays.stream(ProjectPermissionType.values())
            .collect(Collectors.toMap({ obj: ProjectPermissionType -> obj.name },
                    { type: ProjectPermissionType ->
                        Arrays.stream(type.availableScopes)
                                .map { obj: ApiScope -> obj.value }
                                .collect(Collectors.toSet())
                    }
            ))

    @GetMapping(path = ["/scopes"])
    @Operation(summary = "Returns API key scopes")
    @AccessWithApiKey
    fun getApiKeyScopes(): Set<String> {
        val apiKey = authenticationFacade.apiKey
        return apiKey.scopesEnum.asSequence().map { it.value }.toSet()
    }
}
