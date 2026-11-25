package io.tolgee.mcp

import io.tolgee.AbstractSpringTest
import io.tolgee.activity.ActivityHandlerInterceptor
import io.tolgee.security.authentication.AdminAccessInterceptor
import io.tolgee.security.authentication.AuthenticationInterceptor
import io.tolgee.security.authentication.EmailValidationInterceptor
import io.tolgee.security.authentication.GuestAccessInterceptor
import io.tolgee.security.authentication.ReadOnlyModeInterceptor
import io.tolgee.security.authentication.SsoAuthenticationInterceptor
import io.tolgee.security.authorization.FeatureAuthorizationInterceptor
import io.tolgee.security.authorization.OrganizationAuthorizationInterceptor
import io.tolgee.security.authorization.ProjectAuthorizationInterceptor
import io.tolgee.security.ratelimit.RateLimitInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.servlet.HandlerInterceptor

@SpringBootTest
class McpInterceptorCoverageTest : AbstractSpringTest() {
  private val mirroredInMcpRequestContext =
    setOf(
      AuthenticationInterceptor::class.java,
      EmailValidationInterceptor::class.java,
      SsoAuthenticationInterceptor::class.java,
      RateLimitInterceptor::class.java,
      ProjectAuthorizationInterceptor::class.java,
      OrganizationAuthorizationInterceptor::class.java,
      ReadOnlyModeInterceptor::class.java,
      FeatureAuthorizationInterceptor::class.java,
      ActivityHandlerInterceptor::class.java,
    )

  private val notApplicableToMcp =
    setOf<Class<*>>(
      // MCP refuses guests earlier: AuthenticationFacade.authenticatedUserOrNull is null for a guest,
      // so McpRequestContext.requireAuthenticated throws. buildSpec also rejects @AllowGuestAccess twins.
      GuestAccessInterceptor::class.java,
      // Registered only for ADMIN_ENDPOINTS; no MCP tool references one, and admin controllers
      // carry @RequiresSuperAuthentication, which buildSpec rejects.
      AdminAccessInterceptor::class.java,
    )

  /** For modules outside this compile classpath; the billing build runs this test with its beans loaded. */
  private val notApplicableToMcpByName =
    setOf(
      // Marketing attribution read from the tg_sid browser cookie after a response;
      // MCP clients authenticate with tokens and send no cookies, and it checks nothing.
      "io.tolgee.billing.component.marketing.AcquisitionLinkInterceptor",
    )

  @Test
  fun `every HandlerInterceptor is mirrored in McpRequestContext or known not to apply`() {
    val actual =
      applicationContext
        .getBeansOfType(HandlerInterceptor::class.java)
        .values
        .map { it.javaClass as Class<*> }
        .filterNot { it.name in notApplicableToMcpByName }
        .toSet()
    val uncovered = actual - mirroredInMcpRequestContext - notApplicableToMcp
    assertThat(uncovered)
      .withFailMessage(
        "These interceptors never run for MCP tool calls: %s. " +
          "Mirror each one in McpRequestContext, or add it to notApplicableToMcp " +
          "with a reason. Skipping one silently caused GHSA-j998-54vh-rwpg.",
        uncovered.map { it.simpleName },
      ).isEmpty()
  }
}
