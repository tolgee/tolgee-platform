package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.oauth2.OAuth2AuthorizeRequest
import io.tolgee.dtos.request.oauth2.OAuth2ConsentRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.oauth2.ConsentInfoModel
import io.tolgee.hateoas.oauth2.OAuth2AuthorizeResultModel
import io.tolgee.hateoas.oauth2.OAuth2ProjectModel
import io.tolgee.hateoas.oauth2.OAuth2RedirectModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.oauth2.OAuth2AuthorizationService
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.security.oauth2.OAuth2Error
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.OAuth2Redirects
import io.tolgee.security.oauth2.OAuth2Scopes
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.nullIfBlank
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The consent screen's own API. `/oauth2/authorize` is a bare redirect into the SPA, so everything that needs to know
 * *who* is authorizing happens here instead, on the webapp's ordinary JWT — which is why the flow needs no server-side
 * session and no cookie.
 */
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/v2/oauth2")
@OpenApiHideFromPublicDocs
@Tag(name = "OAuth2 flow")
class OAuth2FlowController(
  private val authenticationFacade: AuthenticationFacade,
  private val clientRegistry: OAuth2ClientRegistry,
  private val authorizationService: OAuth2AuthorizationService,
  private val projectService: ProjectService,
  private val securityService: SecurityService,
  private val issuerResolver: OAuth2IssuerResolver,
) : IController {
  @PostMapping("/authorize")
  @Operation(summary = "Record the pending grant the consent screen will act on")
  fun authorize(
    @RequestBody @Valid request: OAuth2AuthorizeRequest,
  ): OAuth2AuthorizeResultModel {
    val userId = authenticationFacade.authenticatedUser.id
    val client = clientRegistry.find(request.clientId) ?: throw NotFoundException(Message.OAUTH_UNKNOWN_CLIENT)
    if (!client.allowsRedirectUri(request.redirectUri)) {
      throw BadRequestException(Message.OAUTH_REDIRECT_URI_NOT_REGISTERED)
    }

    val params =
      OAuth2AuthorizationService.AuthorizeParams(
        responseType = request.responseType.nullIfBlank,
        scope = request.scope.nullIfBlank,
        state = request.state.nullIfBlank,
        codeChallenge = request.codeChallenge.nullIfBlank,
        codeChallengeMethod = request.codeChallengeMethod.nullIfBlank,
      )
    val grant =
      try {
        authorizationService.startAuthorization(
          userId,
          client,
          request.redirectUri,
          params,
          request.project.nullIfBlank,
        )
      } catch (e: OAuth2Error) {
        return OAuth2AuthorizeResultModel(
          consentState = null,
          redirectUrl = OAuth2Redirects.error(request.redirectUri, e, issuerResolver.issuerUrl, params.state),
        )
      }
    return OAuth2AuthorizeResultModel(consentState = grant.consentState, redirectUrl = null)
  }

  @GetMapping("/consent-info")
  @Operation(summary = "Describe the app, capabilities and project being requested, for the consent screen")
  fun consentInfo(
    @RequestParam state: String,
  ): ConsentInfoModel {
    val grant = authorizationService.findOwnPendingByConsentState(state, authenticationFacade.authenticatedUser.id)
    val client = clientRegistry.find(grant.clientId) ?: throw NotFoundException(Message.OAUTH_UNKNOWN_CLIENT)
    val scopes = grant.requestedScopeValues
    val requestedProjectId = grant.projectHint
    return ConsentInfoModel(
      appName = client.name,
      scopes = scopes,
      requiredScopes = client.requiredScopes.map { it.value }.filter { it in scopes },
      project = requestedProjectId?.let { hintedProject(it) },
      requestedProjectId = requestedProjectId,
    )
  }

  @PostMapping("/consent")
  @Operation(summary = "Approve (or, with no scopes, deny) the pending grant")
  @RequiresSuperAuthentication
  fun consent(
    @RequestBody @Valid request: OAuth2ConsentRequest,
  ): OAuth2RedirectModel {
    val approved = request.scopes.orEmpty().flatMap { OAuth2Scopes.splitScopeString(it) }
    val userId = authenticationFacade.authenticatedUser.id
    val resolved = resolveDecision(request, userId, approved)
    val target =
      when (resolved) {
        is OAuth2AuthorizationService.ResolvedConsent.Granted ->
          OAuth2Redirects.code(resolved.redirectUri, resolved.code, issuerResolver.issuerUrl, resolved.clientState)
        is OAuth2AuthorizationService.ResolvedConsent.Refused ->
          OAuth2Redirects.error(resolved.redirectUri, resolved.error, issuerResolver.issuerUrl, resolved.clientState)
      }
    return OAuth2RedirectModel(target)
  }

  private fun hintedProject(projectId: Long): OAuth2ProjectModel? =
    accessibleProject(projectId)?.let { OAuth2ProjectModel(id = it.id, name = it.name) }

  private fun resolveDecision(
    request: OAuth2ConsentRequest,
    userId: Long,
    approved: List<String>,
  ): OAuth2AuthorizationService.ResolvedConsent {
    if (approved.isEmpty()) return authorizationService.denyConsent(request.state, userId)
    val projectIds = requireAccessibleSelection(requireProjectSelection(request))
    return authorizationService.approveConsent(request.state, userId, approved, projectIds)
  }

  private fun requireProjectSelection(request: OAuth2ConsentRequest): Long? {
    if (request.projectScope == null) throw BadRequestException(Message.OAUTH_PROJECT_SCOPE_REQUIRED)
    if (request.projectScope == OAuth2ConsentRequest.ProjectScope.ALL_PROJECTS) return null
    return request.projectId ?: throw BadRequestException(Message.OAUTH_PROJECT_REQUIRED)
  }

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
