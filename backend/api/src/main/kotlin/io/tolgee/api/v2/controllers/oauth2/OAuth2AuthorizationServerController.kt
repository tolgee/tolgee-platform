package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.oauth2.AuthorizationServerMetadataModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.oauth2.OAuth2AuthorizationService
import io.tolgee.security.oauth2.OAuth2Client
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.OAuth2Error
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.OAuth2Redirects
import io.tolgee.security.oauth2.OAuth2Scopes
import io.tolgee.util.nullIfBlank
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
import java.net.URI

/**
 * The OAuth 2.1 authorization server's own endpoints: `/oauth2/authorize`, `/oauth2/token`, `/oauth2/revoke` and
 * RFC 8414 discovery.
 *
 * `/oauth2/authorize` is a browser navigation and carries no credentials of any kind, so it authenticates nobody: it
 * validates only what must be checked before a redirect is safe (the client and its redirect URI) and hands the
 * browser to the consent SPA, which drives the rest over the JWT-authenticated `/v2/oauth2` API.
 */
@RestController
@OpenApiHideFromPublicDocs
@Tag(name = "OAuth2 authorization server")
class OAuth2AuthorizationServerController(
  private val authorizationService: OAuth2AuthorizationService,
  private val clientRegistry: OAuth2ClientRegistry,
  private val issuerResolver: OAuth2IssuerResolver,
  private val frontendUrlProvider: FrontendUrlProvider,
) : IController {
  @GetMapping(OAuth2Constants.AUTHORIZE_PATH)
  @Operation(summary = "OAuth 2.1 authorization endpoint (authorization code + PKCE)")
  fun authorize(
    request: HttpServletRequest,
    @RequestParam("client_id", required = false) clientId: String?,
    @RequestParam("redirect_uri", required = false) redirectUri: String?,
    @RequestParam("response_type", required = false) responseType: String?,
    @RequestParam("scope", required = false) scope: String?,
    @RequestParam("state", required = false) state: String?,
    @RequestParam("code_challenge", required = false) codeChallenge: String?,
    @RequestParam("code_challenge_method", required = false) codeChallengeMethod: String?,
    @RequestParam("project", required = false) project: String?,
  ): ResponseEntity<Any> {
    // Errors here must not redirect: the redirect URI is exactly what has not been validated yet.
    val client = clientId.nullIfBlank?.let { clientRegistry.find(it) } ?: return badRequest("unknown client_id")
    val registeredRedirect =
      redirectUri.nullIfBlank?.takeIf { client.allowsRedirectUri(it) }
        ?: return badRequest("redirect_uri is not registered")
    // OAuth 2.1 §3.1: a parameter sent without a value must be treated as if it were omitted, which is what every
    // nullIfBlank below is for.
    val params =
      OAuth2AuthorizationService.AuthorizeParams(
        responseType = responseType.nullIfBlank,
        scope = scope.nullIfBlank,
        state = state.nullIfBlank,
        codeChallenge = codeChallenge.nullIfBlank,
        codeChallengeMethod = codeChallengeMethod.nullIfBlank,
      )
    if (isRepeated(request, AUTHORIZE_PARAMS)) {
      return redirect(
        OAuth2Redirects.error(registeredRedirect, repeatedParameterError(), issuerResolver.issuerUrl, params.state),
      )
    }
    try {
      authorizationService.validateAuthorizeRequest(params)
    } catch (e: OAuth2Error) {
      return redirect(OAuth2Redirects.error(registeredRedirect, e, issuerResolver.issuerUrl, params.state))
    }
    return redirect(
      consentPageUrl(
        mapOf(
          "client_id" to client.clientId,
          "redirect_uri" to registeredRedirect,
          "response_type" to params.responseType,
          "scope" to params.scope,
          "state" to params.state,
          "code_challenge" to params.codeChallenge,
          "code_challenge_method" to params.codeChallengeMethod,
          "project" to project.nullIfBlank,
        ),
      ),
    )
  }

  // Deliberately not class-level, which the project's CORS convention would otherwise ask for: OAuth 2.1 §3.1 says
  // CORS "MUST NOT be supported at the Authorization Endpoint", which the user agent only ever navigates to. The
  // token, revocation and discovery endpoints below are fetched by the client itself and do need it.
  @CrossOrigin(origins = ["*"])
  @PostMapping(OAuth2Constants.TOKEN_PATH, consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
  @Operation(summary = "OAuth 2.1 token endpoint (authorization_code and refresh_token grants, public clients)")
  fun token(
    request: HttpServletRequest,
    @RequestParam("grant_type", required = false) grantType: String?,
    @RequestParam("client_id", required = false) clientId: String?,
    @RequestParam("code", required = false) code: String?,
    @RequestParam("redirect_uri", required = false) redirectUri: String?,
    @RequestParam("code_verifier", required = false) codeVerifier: String?,
    @RequestParam("refresh_token", required = false) refreshToken: String?,
    @RequestParam("scope", required = false) scope: String?,
  ): ResponseEntity<Map<String, Any>> {
    try {
      val client = requireFormPost(request, TOKEN_PARAMS, clientId)
      val tokens =
        when (grantType.nullIfBlank) {
          "authorization_code" ->
            authorizationService.exchangeCode(
              client,
              code.nullIfBlank,
              redirectUri.nullIfBlank,
              codeVerifier.nullIfBlank,
            )
          "refresh_token" -> authorizationService.refresh(client, refreshToken.nullIfBlank, scope.nullIfBlank)
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
      return errorResponse(e)
    }
  }

  @CrossOrigin(origins = ["*"])
  @PostMapping(OAuth2Constants.REVOKE_PATH, consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
  @Operation(summary = "RFC 7009 token revocation (public clients)")
  fun revoke(
    request: HttpServletRequest,
    @RequestParam("token", required = false) token: String?,
    @RequestParam("client_id", required = false) clientId: String?,
  ): ResponseEntity<Map<String, Any>> {
    try {
      val client = requireFormPost(request, REVOKE_PARAMS, clientId)
      // §2.2.1: a malformed request gets the RFC 6749 §5.2 error, not the 200 that means "your token is not live" —
      // answering 200 here would tell a client its logout succeeded while the grant stays live.
      val presented = token.nullIfBlank ?: throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "token is required")
      authorizationService.revokeToken(client, presented)
      return tokenResponse(HttpStatus.OK).body(emptyMap())
    } catch (e: OAuth2Error) {
      return errorResponse(e)
    }
  }

  @CrossOrigin(origins = ["*"])
  @GetMapping(OAuth2Constants.AUTHORIZATION_SERVER_METADATA_PATH)
  @Operation(summary = "RFC 8414 authorization server metadata")
  fun metadata(): ResponseEntity<AuthorizationServerMetadataModel> {
    if (!clientRegistry.isEnabled) throw NotFoundException()
    val issuer = issuerResolver.issuerUrl
    val model =
      AuthorizationServerMetadataModel(
        issuer = issuer,
        authorizationEndpoint = issuer + OAuth2Constants.AUTHORIZE_PATH,
        tokenEndpoint = issuer + OAuth2Constants.TOKEN_PATH,
        responseTypesSupported = listOf("code"),
        grantTypesSupported = listOf("authorization_code", "refresh_token"),
        codeChallengeMethodsSupported = listOf("S256"),
        tokenEndpointAuthMethodsSupported = listOf("none"),
        authorizationResponseIssParameterSupported = true,
        scopesSupported = OAuth2Scopes.SUPPORTED,
        revocationEndpoint = issuer + OAuth2Constants.REVOKE_PATH,
        revocationEndpointAuthMethodsSupported = listOf("none"),
      )
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(model)
  }

  // RFC 6749 §5.2 error shape, shared by the token and revocation endpoints.
  private fun errorResponse(e: OAuth2Error): ResponseEntity<Map<String, Any>> {
    val body = mutableMapOf<String, Any>("error" to e.error)
    e.description?.let { body["error_description"] = it }
    return tokenResponse(HttpStatus.BAD_REQUEST).body(body)
  }

  // RFC 6749 §5.1: token responses carry credentials and must never be cached.
  private fun tokenResponse(status: HttpStatus): ResponseEntity.BodyBuilder =
    ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_JSON)
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .header(HttpHeaders.PRAGMA, "no-cache")

  /**
   * OAuth 2.1 §3.2.2 and RFC 7009 §2.1 put these parameters in the entity-body. Spring merges the query string into
   * `@RequestParam`, so a code and its verifier — or a live token — could otherwise arrive in the request line, which
   * proxies, access logs and traces record by default.
   */
  private fun requireBodyOnlyParameters(request: HttpServletRequest) {
    if (!request.queryString.isNullOrEmpty()) {
      throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "parameters must be sent in the request body")
    }
  }

  private fun repeatedParameterError() = OAuth2Error(OAuth2Error.INVALID_REQUEST, "a parameter was sent more than once")

  /** OAuth 2.1 §3.1: a spec-defined parameter must not be sent more than once; `getParameter` would silently take the first. */
  private fun isRepeated(
    request: HttpServletRequest,
    names: List<String>,
  ): Boolean = names.any { (request.getParameterValues(it)?.size ?: 0) > 1 }

  private fun redirect(location: String): ResponseEntity<Any> =
    ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build()

  /**
   * RFC 6749 §4.1.2.1: an unregistered client or redirect URI is the one case the server must answer directly rather
   * than redirect, so there is no client to receive a machine-readable error.
   */
  private fun badRequest(description: String): ResponseEntity<Any> =
    ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .contentType(MediaType.TEXT_PLAIN)
      .body("${OAuth2Error.INVALID_REQUEST}: $description")

  /**
   * The shape every form-post endpoint here shares. 401 would need a WWW-Authenticate challenge (RFC 9110 §15.5.2)
   * and there is no token endpoint auth method to issue one, so an unknown client is `invalid_client` with 400.
   */
  private fun requireFormPost(
    request: HttpServletRequest,
    knownParams: List<String>,
    clientId: String?,
  ): OAuth2Client {
    requireBodyOnlyParameters(request)
    if (isRepeated(request, knownParams)) {
      throw repeatedParameterError()
    }
    return clientId.nullIfBlank?.let { clientRegistry.find(it) } ?: throw OAuth2Error(OAuth2Error.INVALID_CLIENT)
  }

  // Relative unless `front-end-url` says otherwise: behind a TLS-terminating proxy the container's view of the
  // request is the internal URL, and redirecting there would strand the user.
  private fun consentPageUrl(params: Map<String, String?>): String {
    val base = frontendUrlProvider.stableUrl?.trimEnd('/').orEmpty()
    return OAuth2Redirects.appendQuery(base + OAuth2Constants.CONSENT_PAGE_PATH, params.toList())
  }

  companion object {
    // Must list every parameter `authorize` forwards to the consent page: a repeat the check does not cover would
    // leave the two ends of the forward disagreeing about which value the user is consenting to.
    private val AUTHORIZE_PARAMS =
      listOf(
        "response_type",
        "client_id",
        "redirect_uri",
        "scope",
        "state",
        "code_challenge",
        "code_challenge_method",
        "project",
      )
    private val TOKEN_PARAMS =
      listOf("grant_type", "client_id", "code", "redirect_uri", "code_verifier", "refresh_token", "scope")

    private val REVOKE_PARAMS = listOf("token", "token_type_hint", "client_id")
  }
}
