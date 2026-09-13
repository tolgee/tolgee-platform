package io.tolgee.security.oauth2

import io.tolgee.api.v2.controllers.oauth2.OAuth2AuthorizationServerController
import io.tolgee.api.v2.controllers.oauth2.ProtectedResourceMetadataController
import io.tolgee.exceptions.NotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class OAuth2MetadataDisabledTest {
  private val disabledRegistry: OAuth2ClientRegistry = mock { on { isEnabled } doReturn false }
  private val issuerResolver: OAuth2IssuerResolver = mock()

  @Test
  fun `the RFC 8414 document is not served, and the issuer is never read`() {
    val controller = OAuth2AuthorizationServerController(mock(), disabledRegistry, issuerResolver, mock())

    assertThatThrownBy { controller.metadata() }.isInstanceOf(NotFoundException::class.java)

    verifyNoInteractions(issuerResolver)
  }

  @Test
  fun `the RFC 9728 document is not served, and the issuer is never read`() {
    val controller = ProtectedResourceMetadataController(issuerResolver, disabledRegistry)

    assertThatThrownBy { controller.mcpDeveloperMetadata() }.isInstanceOf(NotFoundException::class.java)

    verifyNoInteractions(issuerResolver)
  }

  @Test
  fun `a configured deployment does read the issuer`() {
    val enabledRegistry: OAuth2ClientRegistry = mock { on { isEnabled } doReturn true }
    val resolver: OAuth2IssuerResolver = mock { on { issuerUrl } doReturn "https://tolgee.example.com" }

    ProtectedResourceMetadataController(resolver, enabledRegistry).mcpDeveloperMetadata()

    verify(resolver).issuerUrl
  }
}
