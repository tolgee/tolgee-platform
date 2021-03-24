package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.dtos.request.CreateApiKeyDTO
import io.tolgee.dtos.request.EditApiKeyDTO
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.RepositoryPermissionType
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.service.ApiKeyService
import io.tolgee.service.RepositoryService
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/apiKeys")
class ApiKeyController(private val apiKeyService: ApiKeyService, private val repositoryService: RepositoryService) : PrivateController() {
    @Operation(summary = "Get all user's api keys")
    @GetMapping(path = [""])
    fun allByUser(): Set<ApiKeyDTO> {
        return apiKeyService.getAllByUser(authenticationFacade.userAccount).stream()
                .map { apiKey: ApiKey? -> ApiKeyDTO.fromEntity(apiKey) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @GetMapping(path = ["/repository/{repositoryId}"])
    fun allByRepository(@PathVariable("repositoryId") repositoryId: Long?): Set<ApiKeyDTO> {
        securityService.checkRepositoryPermission(repositoryId, RepositoryPermissionType.MANAGE)
        return apiKeyService.getAllByRepository(repositoryId).stream()
                .map { apiKey: ApiKey? -> ApiKeyDTO.fromEntity(apiKey) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @PostMapping(path = [""])
    fun create(@RequestBody @Valid createApiKeyDTO:  CreateApiKeyDTO?): ApiKeyDTO {
        val repository = repositoryService.getById(createApiKeyDTO!!.repositoryId).orElseThrow { NotFoundException(Message.REPOSITORY_NOT_FOUND) }
        securityService.checkApiKeyScopes(createApiKeyDTO.scopes, repository)
        return apiKeyService.createApiKey(authenticationFacade.userAccount, createApiKeyDTO.scopes, repository)
    }

    @PostMapping(path = ["/edit"])
    fun edit(@RequestBody @Valid dto: EditApiKeyDTO?) {
        val apiKey = apiKeyService.getApiKey(dto!!.id).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
        securityService.checkApiKeyScopes(dto.scopes, apiKey.repository)
        apiKey.scopesEnum = dto.scopes
        apiKeyService.editApiKey(apiKey)
    }

    @DeleteMapping(path = ["/{key}"])
    fun delete(@PathVariable("key") key: String?) {
        val apiKey = apiKeyService.getApiKey(key).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
        try {
            securityService.checkRepositoryPermission(apiKey.repository!!.id, RepositoryPermissionType.MANAGE)
        } catch (e: PermissionException) {
            //user can delete their own api keys
            if (apiKey.userAccount!!.id != authenticationFacade.userAccount.id) {
                throw e
            }
        }
        apiKeyService.deleteApiKey(apiKey)
    }

    @GetMapping(path = ["/availableScopes"])
    fun getScopes(): Map<String, Set<String>> = Arrays.stream(RepositoryPermissionType.values())
            .collect(Collectors.toMap({ obj: RepositoryPermissionType -> obj.name },
                    { type: RepositoryPermissionType ->
                        Arrays.stream(type.availableScopes)
                                .map { obj: ApiScope -> obj.value }
                                .collect(Collectors.toSet())
                    }
            ))

    @GetMapping(path = ["/scopes"])
    @AccessWithApiKey
    fun getApiKeyScopes(): Set<String> {
        val apiKey = authenticationFacade.apiKey
        return apiKey.scopesEnum.asSequence().map { it.value }.toSet()
    }
}
