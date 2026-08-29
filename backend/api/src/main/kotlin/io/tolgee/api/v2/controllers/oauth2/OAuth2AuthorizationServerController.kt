package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.hateoas.oauth2.AuthorizationServerMetadataModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.oauth2.OAuth2AuthorizationService
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.OAuth2Error
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.OAuth2Redirects
import io.tolgee.security.oauth2.OAuth2Scopes
import io.tolgee.util.orNullIfBlank
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
 * The OAuth 2.1 authorization server's own endpoints: `/oauth2/authorize`, `/oauth2/token`, and RFC 8414 discovery.
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
  private val tolgeeProperties: TolgeeProperties,
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
    val client = clientId.orNullIfBlank()?.let { clientRegistry.find(it) } ?: return badRequest("unknown client_id")
    val registeredRedirect =
      redirectUri.orNullIfBlank()?.takeIf { client.allowsRedirectUri(it) }
        ?: return badRequest("redirect_uri is not registered")
    // OAuth 2.1 §3.1 and §3.2: a parameter sent without a value must be treated as if it were omitted (what every
    // orNullIfBlank below is for), and must not be sent more than once.
    if (isRepeated(request, AUTHORIZE_PARAMS)) {
      val repeated = OAuth2Error(OAuth2Error.INVALID_REQUEST, "a parameter was sent more than once")
      return redirect(OAuth2Redirects.error(registeredRedirect, repeated, issuerResolver.issuerUrl, state))
    }
    try {
      authorizationService.validateAuthorizeRequest(
        OAuth2AuthorizationService.AuthorizeParams(
          responseType = responseType.orNullIfBlank(),
          scope = scope.orNullIfBlank(),
          state = state.orNullIfBlank(),
          codeChallenge = codeChallenge.orNullIfBlank(),
          codeChallengeMethod = codeChallengeMethod.orNullIfBlank(),
        ),
      )
    } catch (e: OAuth2Error) {
      return redirect(OAuth2Redirects.error(registeredRedirect, e, issuerResolver.issuerUrl, state))
    }
    return redirect(
      consentPageUrl(
        mapOf(
          "client_id" to clientId,
          "redirect_uri" to registeredRedirect,
          "response_type" to responseType,
          "scope" to scope,
          "state" to state,
          "code_challenge" to codeChallenge,
          "code_challenge_method" to codeChallengeMethod,
          "project" to project,
        ),
      ),
    )
  }

  // OAuth 2.1 §3.2 wants CORS here (the client calls it directly) and RFC 8414 wants it on discovery, but §3.1
  // forbids it on the authorization endpoint, which the user agent only ever navigates to.
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
      // OAuth 2.1 §3.2.2: the token request is a form-encoded body. Spring merges the query string into
      // @RequestParam, so a code and its verifier could otherwise arrive in the request line — which proxies,
      // access logs and traces record by default, and which together are a complete grant.
      if (!request.queryString.isNullOrEmpty()) {
        throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "parameters must be sent in the request body")
      }
      if (isRepeated(request, TOKEN_PARAMS)) {
        throw OAuth2Error(OAuth2Error.INVALID_REQUEST, "a parameter was sent more than once")
      }
      // RFC 6749 §5.2's default for an error response is 400. The 401 variant exists only to advertise supported
      // HTTP authentication schemes, and this server has none (discovery says token_endpoint_auth_methods_supported:
      // ["none"]) — a 401 without a WWW-Authenticate challenge would violate RFC 9110 §15.5.2 for no benefit.
      val client =
        clientId.orNullIfBlank()?.let { clientRegistry.find(it) }
          ?: throw OAuth2Error(OAuth2Error.INVALID_CLIENT)
      val tokens =
        when (grantType.orNullIfBlank()) {
          "authorization_code" ->
            authorizationService.exchangeCode(
              client,
              code.orNullIfBlank(),
              redirectUri.orNullIfBlank(),
              codeVerifier.orNullIfBlank(),
            )
          "refresh_token" -> authorizationService.refresh(client, refreshToken.orNullIfBlank(), scope.orNullIfBlank())
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

  @CrossOrigin(origins = ["*"])
  @GetMapping("/.well-known/oauth-authorization-server")
  @Operation(summary = "RFC 8414 authorization server metadata")
  fun metadata(): AuthorizationServerMetadataModel {
    val issuer = issuerResolver.issuerUrl
    return AuthorizationServerMetadataModel(
      issuer = issuer,
      authorizationEndpoint = issuer + OAuth2Constants.AUTHORIZE_PATH,
      tokenEndpoint = issuer + OAuth2Constants.TOKEN_PATH,
      responseTypesSupported = listOf("code"),
      grantTypesSupported = listOf("authorization_code", "refresh_token"),
      codeChallengeMethodsSupported = listOf("S256"),
      tokenEndpointAuthMethodsSupported = listOf("none"),
      authorizationResponseIssParameterSupported = true,
      scopesSupported = OAuth2Scopes.SUPPORTED,
    )
  }

  // RFC 6749 §5.1: token responses carry credentials and must never be cached.
  private fun tokenResponse(status: HttpStatus): ResponseEntity.BodyBuilder =
    ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_JSON)
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .header(HttpHeaders.PRAGMA, "no-cache")

  /** OAuth 2.1 §3.1: a spec-defined parameter must not be sent more than once; `getParameter` would silently take the first. */
  private fun isRepeated(
    request: HttpServletRequest,
    names: List<String>,
  ): Boolean = names.any { (request.getParameterValues(it)?.size ?: 0) > 1 }

  private fun redirect(location: String): ResponseEntity<Any> =
    ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build()

  /**
   * RFC 6749 §4.1.2.1: an unregistered client or redirect URI is the one case the server must answer directly rather
   * than redirect, so there is no client to receive a machine-readable error. The body is for the operator reading
   * the browser, and is deliberately not Tolgee's JSON error shape — no Tolgee client parses it.
   */
  private fun badRequest(description: String): ResponseEntity<Any> =
    ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .contentType(MediaType.TEXT_PLAIN)
      .body("${OAuth2Error.INVALID_REQUEST}: $description")

  /**
   * Relative unless `front-end-url` says otherwise: behind a TLS-terminating proxy the container's view of the
   * request is the internal URL, and redirecting there would strand the user.
   */
  private fun consentPageUrl(params: Map<String, String?>): String {
    val base =
      tolgeeProperties.frontEndUrl
        ?.takeIf { it.isNotBlank() }
        ?.trimEnd('/')
        .orEmpty()
    // Built from the bound parameters, never replayed from the raw query string: RFC 6749 §4.1.1 lets `state` hold
    // any printable ASCII, and a raw replay has to be re-verified before it can be re-emitted.
    return OAuth2Redirects.appendQuery(base + OAuth2Constants.CONSENT_PAGE_PATH, params.toList())
  }

  companion object {
    private val AUTHORIZE_PARAMS =
      listOf("response_type", "client_id", "redirect_uri", "scope", "state", "code_challenge", "code_challenge_method")
    private val TOKEN_PARAMS =
      listOf("grant_type", "client_id", "code", "redirect_uri", "code_verifier", "refresh_token", "scope")
  }
}
