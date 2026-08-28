package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.oauth2.AuthorizationServerMetadataModel
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.oauth2.OAuth2AuthorizationService
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.OAuth2Error
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.OAuth2SessionBootstrapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * The OAuth 2.1 authorization server endpoints: `/oauth2/authorize` (the request, and the consent form the SPA posts
 * back), `/oauth2/token`, and RFC 8414 discovery.
 *
 * The principal for `/oauth2/authorize` comes only from the bootstrapped session, never from a bearer header: a
 * PAK/PAT/OAuth token must not be able to mint another token.
 */
@RestController
@CrossOrigin(origins = ["*"])
@OpenApiHideFromPublicDocs
@Tag(name = "OAuth2 authorization server")
class OAuth2AuthorizationServerController(
  private val authorizationService: OAuth2AuthorizationService,
  private val clientRegistry: OAuth2ClientRegistry,
  private val sessionBootstrapper: OAuth2SessionBootstrapper,
  private val issuerResolver: OAuth2IssuerResolver,
) : IController {
  @GetMapping(AUTHORIZE_PATH)
  @Operation(summary = "OAuth 2.1 authorization endpoint (authorization code + PKCE)")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun authorize(
    request: HttpServletRequest,
    @RequestParam("client_id", required = false) clientId: String?,
    @RequestParam("redirect_uri", required = false) redirectUri: String?,
    @RequestParam("response_type", required = false) responseType: String?,
    @RequestParam("scope", required = false) scope: String?,
    @RequestParam("state", required = false) state: String?,
    @RequestParam("code_challenge", required = false) codeChallenge: String?,
    @RequestParam("code_challenge_method", required = false) codeChallengeMethod: String?,
    @RequestParam(OAuth2Constants.PROJECT_PARAM, required = false) projectHint: String?,
  ): ResponseEntity<Any> {
    val userId = sessionBootstrapper.userIdOf(request) ?: return redirect(bootstrapUrl(request))

    // Errors up to here must not redirect: the redirect URI is exactly what has not been validated yet.
    val client = clientId?.let { clientRegistry.find(it) } ?: return badRequest("unknown client_id")
    if (redirectUri == null ||
      !client.allowsRedirectUri(redirectUri)
    ) {
      return badRequest("redirect_uri is not registered")
    }

    val params =
      OAuth2AuthorizationService.AuthorizeParams(
        responseType = responseType,
        scope = scope,
        state = state,
        codeChallenge = codeChallenge,
        codeChallengeMethod = codeChallengeMethod,
        projectHint = projectHint,
      )
    val authorization =
      try {
        authorizationService.startAuthorization(userId, client, redirectUri, params)
      } catch (e: OAuth2Error) {
        return redirect(errorRedirect(redirectUri, e, state))
      }
    return redirect(consentPageUrl(authorization))
  }

  @PostMapping(AUTHORIZE_PATH)
  @Operation(summary = "Consent form submission: one `scope` field per approved scope, none to deny")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun consent(
    request: HttpServletRequest,
    @RequestParam("client_id", required = false) clientId: String?,
    @RequestParam("state", required = false) state: String?,
    @RequestParam("scope", required = false) scopes: List<String>?,
  ): ResponseEntity<Any> {
    val userId =
      sessionBootstrapper.userIdOf(request) ?: return status(HttpStatus.UNAUTHORIZED, "no authorization session")
    val authorization = state?.let { authorizationService.findByConsentState(it) } ?: return badRequest("unknown state")
    if (authorization.userAccount.id != userId) return badRequest("unknown state")
    if (authorization.clientId != clientId) return badRequest("client_id does not match the authorization")

    val approved = scopes.orEmpty().flatMap { it.split(" ") }.filter { it.isNotBlank() }
    if (approved.isEmpty()) {
      authorizationService.denyConsent(authorization)
      return redirect(
        errorRedirect(authorization.redirectUri, OAuth2Error(OAuth2Error.ACCESS_DENIED), authorization.clientState),
      )
    }
    val code =
      try {
        authorizationService.approveConsent(authorization, approved)
      } catch (e: OAuth2Error) {
        return redirect(errorRedirect(authorization.redirectUri, e, authorization.clientState))
      }

    // The session carried this one authorize → consent round trip; a later connect must re-bootstrap so a token is
    // always minted for whoever is signed into the webapp now, not a stale principal in the cookie.
    request.getSession(false)?.invalidate()
    return redirect(codeRedirect(authorization, code))
  }

  @PostMapping(TOKEN_PATH)
  @Operation(summary = "OAuth 2.1 token endpoint (authorization_code and refresh_token grants, public clients)")
  fun token(
    @RequestParam("grant_type", required = false) grantType: String?,
    @RequestParam("client_id", required = false) clientId: String?,
    @RequestParam("code", required = false) code: String?,
    @RequestParam("redirect_uri", required = false) redirectUri: String?,
    @RequestParam("code_verifier", required = false) codeVerifier: String?,
    @RequestParam("refresh_token", required = false) refreshToken: String?,
    @RequestParam("scope", required = false) scope: String?,
  ): ResponseEntity<Map<String, Any>> {
    try {
      val client =
        clientId?.let { clientRegistry.find(it) }
          ?: throw OAuth2Error(OAuth2Error.INVALID_CLIENT, statusCode = HttpStatus.UNAUTHORIZED.value())
      val tokens =
        when (grantType) {
          "authorization_code" -> authorizationService.exchangeCode(client, code, redirectUri, codeVerifier)
          "refresh_token" -> authorizationService.refresh(client, refreshToken, scope)
          null -> throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "grant_type is required")
          else -> throw OAuth2Error(OAuth2Error.UNSUPPORTED_GRANT_TYPE)
        }
      return tokenResponse(HttpStatus.OK)
        .body(
          mapOf(
            "access_token" to tokens.accessToken,
            "token_type" to "Bearer",
            "expires_in" to tokens.expiresInSeconds,
            "refresh_token" to tokens.refreshToken,
            "scope" to tokens.scopes.joinToString(" "),
          ),
        )
    } catch (e: OAuth2Error) {
      val body = mutableMapOf<String, Any>("error" to e.error)
      e.description?.let { body["error_description"] = it }
      return tokenResponse(HttpStatus.valueOf(e.statusCode)).body(body)
    }
  }

  @GetMapping("/.well-known/oauth-authorization-server")
  @Operation(summary = "RFC 8414 authorization server metadata")
  fun metadata(): AuthorizationServerMetadataModel {
    val issuer = issuerResolver.issuerUrl
    return AuthorizationServerMetadataModel(
      issuer = issuer,
      authorizationEndpoint = issuer + AUTHORIZE_PATH,
      tokenEndpoint = issuer + TOKEN_PATH,
      responseTypesSupported = listOf("code"),
      grantTypesSupported = listOf("authorization_code", "refresh_token"),
      codeChallengeMethodsSupported = listOf("S256"),
      tokenEndpointAuthMethodsSupported = listOf("none"),
      scopesSupported = Scope.entries.map { it.value },
    )
  }

  // RFC 6749 §5.1: token responses carry credentials and must never be cached.
  private fun tokenResponse(status: HttpStatus): ResponseEntity.BodyBuilder =
    ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_JSON)
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .header(HttpHeaders.PRAGMA, "no-cache")

  private fun redirect(location: String): ResponseEntity<Any> =
    ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build()

  private fun badRequest(description: String): ResponseEntity<Any> = status(HttpStatus.BAD_REQUEST, description)

  private fun status(
    status: HttpStatus,
    description: String,
  ): ResponseEntity<Any> =
    ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body("${OAuth2Error.INVALID_REQUEST}: $description")

  /**
   * The bootstrap page turns the webapp JWT into the session this endpoint needs, then continues here. The continue
   * URL is built on the configured issuer, not what the container saw: behind a reverse proxy those differ and the
   * browser can only reach the former.
   */
  private fun bootstrapUrl(request: HttpServletRequest): String {
    val query = request.queryString?.let { "?$it" } ?: ""
    val continueUrl = issuerResolver.issuerUrl + request.requestURI.removePrefix(request.contextPath) + query
    return OAuth2Constants.BOOTSTRAP_PAGE_PATH + "?continue=" + URLEncoder.encode(continueUrl, StandardCharsets.UTF_8)
  }

  private fun consentPageUrl(authorization: OAuth2Authorization): String =
    UriComponentsBuilder
      .fromPath(OAuth2Constants.CONSENT_PAGE_PATH)
      .queryParam("client_id", authorization.clientId)
      .queryParam("scope", authorization.requestedScopes)
      .queryParam("state", authorization.consentState)
      .encode()
      .build()
      .toUriString()

  /** RFC 6749 §4.1.2 success response, plus the RFC 9207 `iss` so a client can tell which server answered. */
  private fun codeRedirect(
    authorization: OAuth2Authorization,
    code: String,
  ): String {
    val builder =
      UriComponentsBuilder
        .fromUriString(authorization.redirectUri)
        .queryParam("code", code)
        .queryParam("iss", issuerResolver.issuerUrl)
    authorization.clientState?.let { builder.queryParam("state", it) }
    return builder.encode().build().toUriString()
  }

  /** RFC 6749 §4.1.2.1 error response. */
  private fun errorRedirect(
    redirectUri: String,
    error: OAuth2Error,
    state: String?,
  ): String {
    val builder = UriComponentsBuilder.fromUriString(redirectUri).queryParam("error", error.error)
    error.description?.let { builder.queryParam("error_description", it) }
    state?.let { builder.queryParam("state", it) }
    return builder.encode().build().toUriString()
  }

  companion object {
    const val AUTHORIZE_PATH = "/oauth2/authorize"
    const val TOKEN_PATH = "/oauth2/token"
  }
}
