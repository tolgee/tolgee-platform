package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.apiKey.CreateApiKeyDto
import io.tolgee.dtos.request.apiKey.RegenerateApiKeyDto
import io.tolgee.dtos.request.apiKey.V2EditApiKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.apiKey.ApiKeyModel
import io.tolgee.hateoas.apiKey.ApiKeyModelAssembler
import io.tolgee.hateoas.apiKey.ApiKeyPermissionsModel
import io.tolgee.hateoas.apiKey.ApiKeyWithLanguagesModel
import io.tolgee.hateoas.apiKey.RevealedApiKeyModel
import io.tolgee.hateoas.apiKey.RevealedApiKeyModelAssembler
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.ApiKey
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.SecurityService
import jakarta.validation.Valid
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

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2")
@Tag(name = "API keys")
class ApiKeyController(
  private val apiKeyService: ApiKeyService,
  private val projectService: ProjectService,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
  private val apiKeyModelAssembler: ApiKeyModelAssembler,
  private val revealedApiKeyModelAssembler: RevealedApiKeyModelAssembler,
  private val projectHolder: ProjectHolder,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<ApiKey>,
  private val permissionService: PermissionService,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
) {
  @PostMapping(path = ["/api-keys"])
  @Operation(summary = "Creates new API key with provided scopes")
  @RequiresSuperAuthentication
  fun create(
    @RequestBody @Valid
    dto: CreateApiKeyDto,
  ): RevealedApiKeyModel {
    val project = projectService.get(dto.projectId)
    if (authenticationFacade.authenticatedUser.role != UserAccount.Role.ADMIN) {
      securityService.checkApiKeyScopes(dto.scopes, project)
    }
    return apiKeyService.create(
      userAccount = authenticationFacade.authenticatedUserEntity,
      scopes = dto.scopes,
      project = project,
      expiresAt = dto.expiresAt,
      description = dto.description,
    ).let {
      revealedApiKeyModelAssembler.toModel(it)
    }
  }

  @Operation(summary = "Returns user's api keys")
  @GetMapping(path = ["/api-keys"])
  fun allByUser(
    pageable: Pageable,
    @RequestParam filterProjectId: Long?,
  ): PagedModel<ApiKeyModel> {
    return apiKeyService.getAllByUser(authenticationFacade.authenticatedUser.id, filterProjectId, pageable)
      .let { pagedResourcesAssembler.toModel(it, apiKeyModelAssembler) }
  }

  @Operation(summary = "Returns specific API key info")
  @GetMapping(path = ["/api-keys/{keyId:[0-9]+}"])
  fun get(
    @PathVariable keyId: Long,
  ): ApiKeyModel {
    val apiKey = apiKeyService.findOptional(keyId).orElseThrow { NotFoundException() }
    if (apiKey.userAccount.id != authenticationFacade.authenticatedUser.id) {
      securityService.checkProjectPermission(apiKey.project.id, Scope.ADMIN)
    }
    return apiKey.let { apiKeyModelAssembler.toModel(it) }
  }

  @GetMapping(path = ["/api-keys/current"])
  @Operation(summary = "Returns current API key info")
  @AllowApiAccess(AuthTokenType.ONLY_PAK)
  fun getCurrent(): ApiKeyWithLanguagesModel {
    if (!authenticationFacade.isProjectApiKeyAuth) {
      throw BadRequestException(Message.INVALID_AUTHENTICATION_METHOD)
    }

    val apiKey = authenticationFacade.projectApiKeyEntity

    val permissionData =
      permissionService.getProjectPermissionData(
        apiKey.project.id,
        authenticationFacade.authenticatedUser.id,
      )

    val translateLanguageIds =
      permissionData.computedPermissions.translateLanguageIds.toNormalizedPermittedLanguageSet()

    return ApiKeyWithLanguagesModel(
      apiKeyModelAssembler.toModel(apiKey),
      permittedLanguageIds = translateLanguageIds,
    )
  }

  @GetMapping(path = ["/api-keys/current-permissions"])
  @Operation(summary = "Returns current PAK or PAT permissions for current user, api-key and project")
  @AllowApiAccess()
  fun getCurrentPermissions(
    @RequestParam
    @Parameter(description = "Required when using with PAT")
    projectId: Long?,
  ): ApiKeyPermissionsModel {
    val apiKeyAuthentication = authenticationFacade.isProjectApiKeyAuth
    val personalAccessTokenAuth = authenticationFacade.isPersonalAccessTokenAuth

    val projectIdNotNull =
      when {
        apiKeyAuthentication ->
          authenticationFacade.projectApiKey.projectId

        personalAccessTokenAuth ->
          projectId ?: throw BadRequestException(Message.NO_PROJECT_ID_PROVIDED)

        else -> throw BadRequestException(Message.INVALID_AUTHENTICATION_METHOD)
      }

    val permissionData =
      permissionService.getProjectPermissionData(
        projectIdNotNull,
        authenticationFacade.authenticatedUser.id,
      )

    val computed = permissionData.computedPermissions
    val scopes =
      when {
        apiKeyAuthentication -> authenticationFacade.projectApiKey.scopes.toTypedArray()
        else -> computed.scopes
      }

    return ApiKeyPermissionsModel(
      projectIdNotNull,
      type = if (apiKeyAuthentication) null else computed.type,
      translateLanguageIds = computed.translateLanguageIds.toNormalizedPermittedLanguageSet(),
      viewLanguageIds = computed.viewLanguageIds.toNormalizedPermittedLanguageSet(),
      stateChangeLanguageIds = computed.stateChangeLanguageIds.toNormalizedPermittedLanguageSet(),
      scopes = scopes,
      project = simpleProjectModelAssembler.toModel(projectService.get(projectIdNotNull)),
    )
  }

  fun Set<Long>?.toNormalizedPermittedLanguageSet(): Set<Long>? {
    if (this.isNullOrEmpty()) {
      return null
    }
    return this.toSet()
  }

  @GetMapping(path = ["/projects/{projectId:[0-9]+}/api-keys"])
  @Operation(summary = "Returns all API keys for project")
  @RequiresProjectPermissions([Scope.ADMIN])
  fun allByProject(pageable: Pageable): PagedModel<ApiKeyModel> {
    return apiKeyService.getAllByProject(projectHolder.project.id, pageable)
      .let { pagedResourcesAssembler.toModel(it, apiKeyModelAssembler) }
  }

  @PutMapping(path = ["/api-keys/{apiKeyId:[0-9]+}"])
  @Operation(summary = "Edits existing API key")
  @RequiresSuperAuthentication
  fun update(
    @RequestBody @Valid
    dto: V2EditApiKeyDto,
    @PathVariable apiKeyId: Long,
  ): ApiKeyModel {
    val apiKey = apiKeyService.get(apiKeyId)
    checkOwner(apiKey)
    securityService.checkApiKeyScopes(dto.scopes, apiKey.project)
    apiKey.scopesEnum = dto.scopes.toMutableSet()
    return apiKeyService.editApiKey(apiKey, dto).let { apiKeyModelAssembler.toModel(it) }
  }

  @PutMapping(value = ["/api-keys/{apiKeyId:[0-9]+}/regenerate"])
  @Operation(
    summary = "Regenerates API key. It generates new API key value and updates its time of expiration.",
  )
  @RequiresSuperAuthentication
  fun regenerate(
    @RequestBody @Valid
    dto: RegenerateApiKeyDto,
    @PathVariable apiKeyId: Long,
  ): RevealedApiKeyModel {
    checkOwner(apiKeyId)
    val regenerated = apiKeyService.regenerate(apiKeyId, dto.expiresAt)
    return revealedApiKeyModelAssembler.toModel(regenerated)
  }

  @DeleteMapping(path = ["/api-keys/{apiKeyId:[0-9]+}"])
  @Operation(summary = "Deletes API key")
  @RequiresSuperAuthentication
  fun delete(
    @PathVariable apiKeyId: Long,
  ) {
    val apiKey = apiKeyService.findOptional(apiKeyId).orElseThrow { NotFoundException(Message.API_KEY_NOT_FOUND) }
    checkOwner(apiKey)
    apiKeyService.deleteApiKey(apiKey)
  }

  private fun checkOwner(id: Long) {
    checkOwner(apiKeyService.get(id))
  }

  private fun checkOwner(apiKey: ApiKey) {
    try {
      securityService.checkProjectPermission(apiKey.project.id, Scope.ADMIN)
    } catch (e: PermissionException) {
      // users can delete their own api keys
      if (apiKey.userAccount.id != authenticationFacade.authenticatedUser.id) {
        throw e
      }
    }
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
            schema =
              Schema(
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
            }""",
              ),
          ),
        ],
      ),
    ],
  )
  @Deprecated(message = "Don't use this endpoint, it's useless.")
  val scopes: Map<String, List<String>> by lazy {
    ProjectPermissionType.values()
      .associate { it -> it.name to it.availableScopes.map { it.value }.toList() }
  }
}
