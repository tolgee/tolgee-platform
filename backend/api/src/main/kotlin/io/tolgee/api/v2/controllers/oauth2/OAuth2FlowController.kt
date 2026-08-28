package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.oauth2.ConsentInfoModel
import io.tolgee.hateoas.oauth2.OAuth2ProjectModel
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.oauth2.OAuth2AuthorizationService
import io.tolgee.security.oauth2.OAuth2Client
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.security.oauth2.OAuth2SessionBootstrapper
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
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
// Drives the consent screen only: authenticated by the webapp JWT, and keyed by a `state` only the SPA holds. Not part
// of the public API.
@OpenApiHideFromPublicDocs
@Tag(name = "OAuth2 flow")
class OAuth2FlowController(
  private val authenticationFacade: AuthenticationFacade,
  private val clientRegistry: OAuth2ClientRegistry,
  private val authorizationService: OAuth2AuthorizationService,
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
    val client = clientRegistry.find(clientId) ?: throw NotFoundException()
    val authorization = state?.let { ownAuthorization(it) }
    val scopes = requestedScopes(authorization, scope)
    val requiredScopes = clientRequiredScopes(client).filter { it in scopes }
    val requestedProjectId = authorization?.projectHint
    return ConsentInfoModel(
      appName = client.name,
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
    authorization.bindProjects(requireAccessibleSelection(projectId))
    authorizationService.save(authorization)
  }

  // From the stored request, not the `scope` parameter: the screen has to describe the authorization it is consenting
  // to. The parameter is only a fallback for a state that no longer resolves.
  private fun requestedScopes(
    authorization: OAuth2Authorization?,
    scopeParam: String?,
  ): List<String> {
    authorization?.requestedScopeValues?.takeIf { it.isNotEmpty() }?.let { return it }
    return scopeParam?.let { splitScopeString(it) } ?: emptyList()
  }

  private fun clientRequiredScopes(client: OAuth2Client): List<String> = client.requiredScopes.map { it.value }

  private fun splitScopeString(raw: String): List<String> = raw.split(" ").filter { it.isNotBlank() }

  private fun hintedProject(projectId: Long): OAuth2ProjectModel? =
    accessibleProject(projectId)?.let { OAuth2ProjectModel(id = it.id, name = it.name) }

  // Only the caller's own pending authorization: `state` is guessable enough that another user's must not resolve.
  private fun ownAuthorization(state: String): OAuth2Authorization? {
    val authorization = authorizationService.findByConsentState(state) ?: return null
    if (authorization.userAccount.id != authenticationFacade.authenticatedUser.id) return null
    return authorization
  }

  /** Null means every project the user can reach; a project id must be one the user can actually use. */
  private fun requireAccessibleSelection(projectId: Long?): List<Long>? {
    if (projectId == null) return null
    if (accessibleProject(projectId) == null) throw PermissionException()
    return listOf(projectId)
  }

  // Existence first: the permission lookup 404s a missing project, which would 404 the consent screen on a stale hint.
  private fun accessibleProject(projectId: Long): ProjectDto? {
    val dto = projectService.findDto(projectId) ?: return null
    if (securityService.getCurrentPermittedScopes(projectId).isEmpty()) return null
    return dto
  }
}
