package io.tolgee.mcp.tools.spec

import io.tolgee.exceptions.AuthenticationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever

class CheckAuthenticationTest : McpToolEndpointSpecTestBase() {
  @Test
  fun `no authenticated user throws AuthenticationException`() {
    whenever(authenticationFacade.authenticatedUserOrNull).thenReturn(null)

    assertThatThrownBy { sut.executeAs(spec()) {} }
      .isInstanceOf(AuthenticationException::class.java)
  }
}
