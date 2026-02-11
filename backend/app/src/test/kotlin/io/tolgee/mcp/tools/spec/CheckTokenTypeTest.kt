package io.tolgee.mcp.tools.spec

import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authentication.AuthTokenType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever

class CheckTokenTypeTest : McpSecurityTestBase() {
  @Test
  fun `non-API auth skips token type check`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(false)

    sut.executeAs(spec(allowedTokenType = AuthTokenType.ONLY_PAT)) {}
  }

  @Test
  fun `PAK auth with ONLY_PAT throws PAK_ACCESS_NOT_ALLOWED`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isProjectApiKeyAuth).thenReturn(true)

    assertThatThrownBy {
      sut.executeAs(spec(allowedTokenType = AuthTokenType.ONLY_PAT)) {}
    }.isInstanceOf(PermissionException::class.java)
      .satisfies({ ex ->
        assertThat((ex as PermissionException).tolgeeMessage).isEqualTo(Message.PAK_ACCESS_NOT_ALLOWED)
      })
  }

  @Test
  fun `PAT auth with ONLY_PAK throws PAT_ACCESS_NOT_ALLOWED`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isPersonalAccessTokenAuth).thenReturn(true)

    assertThatThrownBy {
      sut.executeAs(spec(allowedTokenType = AuthTokenType.ONLY_PAK)) {}
    }.isInstanceOf(PermissionException::class.java)
      .satisfies({ ex ->
        assertThat((ex as PermissionException).tolgeeMessage).isEqualTo(Message.PAT_ACCESS_NOT_ALLOWED)
      })
  }

  @Test
  fun `PAK auth with ANY passes`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isProjectApiKeyAuth).thenReturn(true)

    sut.executeAs(spec(allowedTokenType = AuthTokenType.ANY)) {}
  }

  @Test
  fun `PAT auth with ANY passes`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isPersonalAccessTokenAuth).thenReturn(true)

    sut.executeAs(spec(allowedTokenType = AuthTokenType.ANY)) {}
  }

  @Test
  fun `PAK auth with ONLY_PAK passes`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isProjectApiKeyAuth).thenReturn(true)

    sut.executeAs(spec(allowedTokenType = AuthTokenType.ONLY_PAK)) {}
  }

  @Test
  fun `PAT auth with ONLY_PAT passes`() {
    whenever(authenticationFacade.isApiAuthentication).thenReturn(true)
    whenever(authenticationFacade.isPersonalAccessTokenAuth).thenReturn(true)

    sut.executeAs(spec(allowedTokenType = AuthTokenType.ONLY_PAT)) {}
  }
}
