package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.OAuth2ConsentE2eData
import io.tolgee.security.authentication.JwtService
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/oauth2-consent"])
class OAuth2ConsentE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var userAccounts: UserAccountService

  override val testData: TestDataBuilder
    get() = OAuth2ConsentE2eData().root

  /**
   * Where the authorization code is delivered in e2e.
   *
   * The redirect target must not be a webapp route: the SPA redirects an unrecognized path to the dashboard, which
   * would drop the `code` query parameter before the test could read it. This endpoint just terminates the redirect so
   * the browser stays on a URL carrying the code.
   */
  @GetMapping(value = ["/callback"])
  fun callback(): String = "oauth2 e2e callback"

  /**
   * Where a test must address this fixture to use it as a CIMD client.
   *
   * Two constraints meet here and neither spelling is free. The backend fetches the document itself, so the URL has
   * to reach the connector from inside the container — the published host port does not. And
   * [io.tolgee.security.oauth2.cimd.CimdClientPolicy] refuses a `client_id` on one of this instance's own origins,
   * which `tolgee.frontend-url` spells with `localhost`. The loopback IP literal on the connector's own port
   * satisfies both.
   */
  @GetMapping(value = ["/cimd-base-url"])
  fun cimdBaseUrl(request: HttpServletRequest): Map<String, String> =
    mapOf("baseUrl" to "http://127.0.0.1:${request.localPort}$CIMD_BASE_PATH")

  /**
   * A Client ID Metadata Document, so the CIMD path can be driven end to end: the `client_id` a test presents is this
   * very URL, and the document has to name that same URL back. `disable-url-ssrf-protection` is on in the e2e profile,
   * which is what lets the fetcher reach an http loopback host at all.
   *
   * `logo_uri` points at a path this fixture does not serve, and that is the point: Tolgee never fetches it. See
   * `CimdMetadataValidationTest`, which pins that the field is neither read nor a reason to refuse the document.
   */
  @GetMapping(value = ["/cimd-client"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun cimdClient(request: HttpServletRequest): String {
    val base = "${request.scheme}://${request.serverName}:${request.serverPort}$CIMD_BASE_PATH"
    return """
      {
        "client_id": "$base/cimd-client",
        "client_name": "E2E Unverified App",
        "token_endpoint_auth_method": "none",
        "grant_types": ["authorization_code"],
        "redirect_uris": ["$base/callback", "$LOOPBACK_REDIRECT_URI"],
        "logo_uri": "$base/logo.png"
      }
      """.trimIndent()
  }

  /**
   * A token for the fixture's user that never passed a password check, which logging in cannot produce — `/generatetoken`
   * always answers with a super one.
   */
  @GetMapping(value = ["/non-super-jwt"])
  fun nonSuperJwt(): Map<String, String> {
    val user = userAccounts.get(OAuth2ConsentE2eData.USERNAME)
    return mapOf("jwt" to jwtService.emitToken(user.id, isSuper = false))
  }

  companion object {
    const val CIMD_BASE_PATH = "/internal/e2e-data/oauth2-consent"

    /** Lets one test present this client with a loopback redirect, which is the case where both banners would fire. */
    const val LOOPBACK_REDIRECT_URI = "http://127.0.0.1:53211/callback"
  }
}
