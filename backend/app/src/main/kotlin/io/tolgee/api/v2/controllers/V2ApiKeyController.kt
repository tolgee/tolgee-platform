package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.apiKey.ApiKeyModel
import io.tolgee.api.v2.hateoas.apiKey.ApiKeyModelAssembler
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apiKey.CreateApiKeyDto
import io.tolgee.dtos.request.apiKey.V2EditApiKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2")
@Tag(name = "API keys")
class V2ApiKeyController(
  private val apiKeyService: io.tolgee.service.ApiKeyService,
  private val projectService: ProjectService,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
  private val apiKeyModelAssembler: ApiKeyModelAssembler,
  private val projectHolder: ProjectHolder,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<ApiKey>
) {

  @PostMapping(path = ["/api-keys"])
  @Operation(summary = "Creates new API key with provided scopes")
  fun create(@RequestBody @Valid dto: CreateApiKeyDto): ApiKeyModel {
    val project = projectService.get(dto.projectId)
    securityService.checkApiKeyScopes(dto.scopes, project)
    return apiKeyService.create(authenticationFacade.userAccountEntity, dto.scopes, project!!).let {
      apiKeyModelAssembler.toModel(it)
    }
  }

  @Operation(summary = "Returns user's api keys")
  @GetMapping(path = ["/api-keys"])
  @AccessWithAnyProjectPermission
  fun allByUser(pageable: Pageable, @RequestParam filterProjectId: Long?): PagedModel<ApiKeyModel> {
    return apiKeyService.getAllByUser(authenticationFacade.userAccount.id, filterProjectId, pageable)
      .let { pagedResourcesAssembler.toModel(it, apiKeyModelAssembler) }
  }

  @Operation(summary = "Returns specific API key info")
  @GetMapping(path = ["/api-keys/{keyId:[0-9]+}"])
  fun get(@PathVariable keyId: Long): ApiKeyModel {
    val apiKey = apiKeyService.getApiKey(keyId).orElseThrow { NotFoundException() }
    if (apiKey.userAccount.id != authenticationFacade.userAccount.id) {
      securityService.checkProjectPermission(apiKey.project.id, ProjectPermissionType.MANAGE)
    }
    return apiKey.let { apiKeyModelAssembler.toModel(it) }
  }

  @GetMapping(path = ["/api-keys/current"])
  @Operation(summary = "Returns current API key info")
  @AccessWithApiKey
  fun getCurrent(): ApiKeyModel {
    val apiKey = authenticationFacade.apiKey
    return apiKeyModelAssembler.toModel(apiKey)
  }

  @GetMapping(path = ["/projects/{projectId:[0-9]+}/api-keys"])
  @Operation(summary = "Returns all API keys for project")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun allByProject(pageable: Pageable): PagedModel<ApiKeyModel> {
    return apiKeyService.getAllByProject(projectHolder.project.id, pageable)
      .let { pagedResourcesAssembler.toModel(it, apiKeyModelAssembler) }
  }

  @PutMapping(path = ["/api-keys/{apiKeyId:[0-9]+}"])
  @Operation(summary = "Edits existing API key")
  fun update(@RequestBody @Valid dto: V2EditApiKeyDto, @PathVariable apiKeyId: Long): ApiKeyModel {
    val apiKey = apiKeyService.getApiKey(apiKeyId).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
    securityService.checkApiKeyScopes(dto.scopes, apiKey.project)
    apiKey.scopesEnum = dto.scopes.toMutableSet()
    return apiKeyService.editApiKey(apiKey).let { apiKeyModelAssembler.toModel(it) }
  }

  @DeleteMapping(path = ["/api-keys/{apiKeyId:[0-9]+}"])
  @Operation(summary = "Deletes API key")
  fun delete(@PathVariable apiKeyId: Long) {
    val apiKey = apiKeyService.getApiKey(apiKeyId).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
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

  @get:GetMapping(path = ["/api-keys/availableScopes"])
  @get:Operation(
    summary = "Returns API key scopes for every permission type",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(
              example = """
            {
              "TRANSLATE":[
                "translations.view",
                "translations.edit",
                "screenshots.view"
              ],
              "MANAGE":[
                "translations.view",
                "translations.edit",
                "keys.edit",
                "screenshots.view",
                "screenshots.upload",
                "screenshots.delete"
              ],
              "EDIT":[
                "translations.view",
                "translations.edit",
                "keys.edit",
                "screenshots.view",
                "screenshots.upload",
                "screenshots.delete"
              ],
              "VIEW":[
                "translations.view",
                "screenshots.view"
              ]
            }"""
            )
          )
        ]
      )
    ]
  )
  val scopes: Map<String, List<String>> by lazy {
    ProjectPermissionType.values()
      .associate { it -> it.name to it.availableScopes.map { it.value }.toList() }
  }
}
