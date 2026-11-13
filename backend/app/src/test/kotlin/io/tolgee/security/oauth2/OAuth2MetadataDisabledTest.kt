package io.tolgee.security.oauth2

import io.tolgee.api.v2.controllers.oauth2.OAuth2AuthorizationServerController
import io.tolgee.api.v2.controllers.oauth2.ProtectedResourceMetadataController
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.exceptions.NotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class OAuth2MetadataDisabledTest {
  private val unconfigured: OAuth2IssuerResolver = mock { on { isConfigured } doReturn false }

  @Test
  fun `the RFC 8414 document is not served, and the throwing issuer getter is never reached`() {
    val controller =
      OAuth2AuthorizationServerController(mock(), mock(), unconfigured, mock(), mock(), OAuth2ServerProperties())

    assertThatThrownBy { controller.metadata() }.isInstanceOf(NotFoundException::class.java)

    verify(unconfigured, never()).issuerUrl
  }

  @Test
  fun `the RFC 9728 document is not served, and the throwing issuer getter is never reached`() {
    val controller = ProtectedResourceMetadataController(unconfigured, OAuth2Resources(unconfigured))

    assertThatThrownBy { controller.mcpDeveloperMetadata() }.isInstanceOf(NotFoundException::class.java)

    verify(unconfigured, never()).issuerUrl
  }

  @Test
  fun `a configured deployment does read the issuer`() {
    val resolver: OAuth2IssuerResolver =
      mock {
        on { isConfigured } doReturn true
        on { issuerUrl } doReturn "https://tolgee.example.com"
      }

    ProtectedResourceMetadataController(resolver, OAuth2Resources(resolver)).mcpDeveloperMetadata()

    verify(resolver, atLeastOnce()).issuerUrl
  }
}
