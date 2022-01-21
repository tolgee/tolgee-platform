package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apiKey.CreateApiKeyDto
import io.tolgee.dtos.request.apiKey.EditApiKeyDTO
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.stream.Collectors
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/apiKeys")
@Tag(name = "API keys")
class ApiKeyController(
  private val apiKeyService: io.tolgee.service.ApiKeyService,
  private val projectService: ProjectService,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService
) {
  @AccessWithAnyProjectPermission
  @Operation(summary = "Returns all user's api keys")
  @GetMapping(path = [""])
  fun allByUser(): Set<ApiKeyDTO> {
    return apiKeyService.getAllByUser(authenticationFacade.userAccount.id)
      .map { apiKey: ApiKey -> ApiKeyDTO.fromEntity(apiKey) }
      .toCollection(linkedSetOf())
  }

  @GetMapping(path = ["/project/{projectId}"])
  @Operation(summary = "Returns all API keys for project")
  fun allByProject(@PathVariable("projectId") repositoryId: Long?): Set<ApiKeyDTO> {
    securityService.checkProjectPermission(repositoryId!!, ProjectPermissionType.MANAGE)
    return apiKeyService.getAllByProject(repositoryId)
      .map { apiKey: ApiKey -> ApiKeyDTO.fromEntity(apiKey) }
      .toCollection(linkedSetOf())
  }

  @PostMapping(path = [""])
  @Operation(summary = "Creates new API key with provided scopes")
  fun create(@RequestBody @Valid createApiKeyDTO: CreateApiKeyDto?): ApiKeyDTO {
    val project = projectService.get(createApiKeyDTO!!.projectId)
    securityService.checkApiKeyScopes(createApiKeyDTO.scopes, project)
    return ApiKeyDTO.fromEntity(
      apiKeyService.create(authenticationFacade.userAccountEntity, createApiKeyDTO.scopes, project!!)
    )
  }

  @PostMapping(path = ["/edit"])
  @Operation(summary = "Edits existing API key")
  fun edit(@RequestBody @Valid dto: EditApiKeyDTO) {
    val apiKey = apiKeyService.getApiKey(dto.id)
      .orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
    securityService.checkApiKeyScopes(dto.scopes, apiKey.project)
    apiKey.scopesEnum = dto.scopes.toMutableSet()
    apiKeyService.editApiKey(apiKey)
  }

  @DeleteMapping(path = ["/{key}"])
  @Operation(summary = "Deletes API key")
  fun delete(@PathVariable("key") key: String) {
    val apiKey = apiKeyService.getApiKey(key)
      .orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
    try {
      securityService.checkProjectPermission(apiKey.project.id, ProjectPermissionType.MANAGE)
    } catch (e: PermissionException) {
      // users can delete their own api keys
      if (apiKey.userAccount.id != authenticationFacade.userAccount.id) {
        throw e
      }
    }
    apiKeyService.deleteApiKey(apiKey)
  }

  @GetMapping(path = ["/availableScopes"])
  @Operation(summary = "Returns API key scopes for every permission type")
  fun getScopes(): Map<String, List<String>> = Arrays.stream(ProjectPermissionType.values())
    .collect(
      Collectors.toMap(
        { obj: ProjectPermissionType -> obj.name },
        { type: ProjectPermissionType ->
          Arrays.stream(type.availableScopes)
            .map { obj: ApiScope -> obj.value }
            .collect(Collectors.toList())
        }
      )
    )

  @GetMapping(path = ["/scopes"])
  @Operation(summary = "Returns API key scopes")
  @AccessWithApiKey
  fun getApiKeyScopes(): Set<String> {
    val apiKey = authenticationFacade.apiKey
    return apiKey.scopesEnum.asSequence().map { it.value }.toSet()
  }
}
