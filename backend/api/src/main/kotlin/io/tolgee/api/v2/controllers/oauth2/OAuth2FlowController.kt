package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.oauth2.ConsentInfoModel
import io.tolgee.hateoas.oauth2.OAuth2ProjectModel
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.OAuth2SessionBootstrapper
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.projectHint
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/oauth2")
@Tag(name = "OAuth2 flow")
class OAuth2FlowController(
  private val authenticationFacade: AuthenticationFacade,
  private val registeredClientRepository: RegisteredClientRepository,
  private val oAuth2AuthorizationService: OAuth2AuthorizationService,
  private val projectService: ProjectService,
  private val securityService: SecurityService,
  private val oAuth2SessionBootstrapper: OAuth2SessionBootstrapper,
) : IController {
  @PostMapping("/session-bootstrap")
  @Operation(summary = "Establish an HTTP session from the current JWT for the OAuth2 authorization flow")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun bootstrap(request: HttpServletRequest) {
    val userId = authenticationFacade.authenticatedUserOrNull?.id ?: throw PermissionException()
    oAuth2SessionBootstrapper.establishSession(request, userId)
  }

  @GetMapping("/consent-info")
  @Operation(summary = "Describe the app, capabilities and project being requested, for the consent screen")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun consentInfo(
    @RequestParam clientId: String,
    @RequestParam(required = false) scope: String?,
    @RequestParam(required = false) state: String?,
  ): ConsentInfoModel {
    val client = registeredClientRepository.findByClientId(clientId) ?: throw NotFoundException()
    val scopes = scope?.let { splitScopeString(it) } ?: emptyList()
    val requiredScopes = clientRequiredScopes(client).filter { it in scopes }
    val requestedProjectId = state?.let { ownAuthorization(it)?.projectHint() }
    return ConsentInfoModel(
      appName = client.clientName,
      scopes = scopes,
      requiredScopes = requiredScopes,
      project = requestedProjectId?.let { hintedProject(it) },
      requestedProjectId = requestedProjectId,
    )
  }

  @PostMapping("/select-project")
  @Operation(summary = "Bind the pending authorization to the project chosen on the consent screen")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun selectProject(
    @RequestParam state: String,
    @RequestParam(required = false) projectId: Long?,
  ) {
    val authorization = ownAuthorization(state) ?: throw NotFoundException()
    val selection = projectSelectionValue(projectId)
    oAuth2AuthorizationService.save(
      OAuth2Authorization.from(authorization).attribute(OAuth2Constants.PROJECT_ATTRIBUTE, selection).build(),
    )
  }

  private fun clientRequiredScopes(client: RegisteredClient): List<String> {
    val raw = client.clientSettings.settings[OAuth2Constants.REQUIRED_SCOPES_SETTING] as? String ?: return emptyList()
    return splitScopeString(raw)
  }

  private fun splitScopeString(raw: String): List<String> = raw.split(" ").filter { it.isNotBlank() }

  /** Resolves the hinted project's name only when the user has access, so an unrelated hint can't leak a name. */
  private fun hintedProject(projectId: Long): OAuth2ProjectModel? =
    accessibleProject(projectId)?.let { projectModel(it.id, it.name) }

  private fun projectModel(
    id: Long,
    name: String?,
  ) = OAuth2ProjectModel(id = id, name = name ?: "#$id")

  /** The pending authorization for [state], only when it belongs to the caller (guards against state replay). */
  private fun ownAuthorization(state: String): OAuth2Authorization? {
    val authorization =
      oAuth2AuthorizationService.findByToken(state, OAuth2TokenType(OAuth2ParameterNames.STATE)) ?: return null
    if (authorization.principalName != authenticationFacade.authenticatedUser.id.toString()) return null
    return authorization
  }

  private fun projectSelectionValue(projectId: Long?): String {
    if (projectId == null) return OAuth2Constants.ALL_PROJECTS
    if (accessibleProject(projectId) == null) throw PermissionException()
    return projectId.toString()
  }

  // Existence first: the permission lookup 404s a missing project, which would 404 the consent screen on a stale hint.
  private fun accessibleProject(projectId: Long): ProjectDto? {
    val dto = projectService.findDto(projectId) ?: return null
    if (securityService.getCurrentPermittedScopes(projectId).isEmpty()) return null
    return dto
  }
}
