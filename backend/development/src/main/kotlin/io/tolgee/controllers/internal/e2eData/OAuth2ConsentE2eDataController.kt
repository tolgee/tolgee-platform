package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.OAuth2ConsentE2eData
import io.tolgee.security.authentication.JwtService
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Autowired
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
   * A token for the fixture's user that never passed a password check, which logging in cannot produce — `/generatetoken`
   * always answers with a super one.
   */
  @GetMapping(value = ["/non-super-jwt"])
  fun nonSuperJwt(): Map<String, String> {
    val user = userAccounts.get(OAuth2ConsentE2eData.USERNAME)
    return mapOf("jwt" to jwtService.emitToken(user.id, isSuper = false))
  }
}
