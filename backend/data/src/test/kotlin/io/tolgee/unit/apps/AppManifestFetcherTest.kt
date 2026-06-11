package io.tolgee.unit.apps

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.apps.AppManifestFetcher
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestTemplate

class AppManifestFetcherTest {
  private val manifestJson =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "scopes": ["translations.view"],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()

  private fun fetcher(allowLocalAddresses: Boolean): AppManifestFetcher {
    val restTemplate = mock<RestTemplate>()
    doReturn(manifestJson).whenever(restTemplate).getForObject(anyString(), eq(String::class.java))
    return AppManifestFetcher(
      restTemplate,
      jacksonObjectMapper(),
      UrlSecurity(InternalProperties()),
      AppsProperties().apply { this.allowLocalAddresses = allowLocalAddresses },
    )
  }

  @Test
  fun `rejects a local manifest URL by default`() {
    val exception =
      assertThrows<BadRequestException> {
        fetcher(allowLocalAddresses = false).fetch("http://localhost:5181/manifest.json")
      }
    exception.code.assert.isEqualTo(Message.URL_NOT_VALID.code)
  }

  @Test
  fun `allows a local manifest URL when apps allowLocalAddresses is true`() {
    assertDoesNotThrow {
      fetcher(allowLocalAddresses = true).fetch("http://localhost:5181/manifest.json")
    }
  }
}
