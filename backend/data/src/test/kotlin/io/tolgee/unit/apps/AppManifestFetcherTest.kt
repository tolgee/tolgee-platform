package io.tolgee.unit.apps

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.apps.AppManifestFetcher
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper

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

  private val httpClient =
    mock<AppManifestHttpClient>().apply {
      doReturn(manifestJson).whenever(this).fetchBody(anyString())
    }

  private fun fetcher(allowLocalAddresses: Boolean): AppManifestFetcher {
    return AppManifestFetcher(
      httpClient,
      jacksonObjectMapper(),
      UrlSecurity(InternalProperties()),
      AppsProperties().apply { this.allowLocalAddresses = allowLocalAddresses },
    )
  }

  private fun fetcherReturning(json: String): AppManifestFetcher {
    val client =
      mock<AppManifestHttpClient>().apply {
        doReturn(json).whenever(this).fetchBody(anyString())
      }
    return AppManifestFetcher(
      client,
      jacksonObjectMapper(),
      UrlSecurity(InternalProperties()),
      AppsProperties().apply { this.allowLocalAddresses = true },
    )
  }

  @Test
  fun `rejects a local manifest URL by default, without ever fetching it`() {
    val exception =
      assertThrows<BadRequestException> {
        fetcher(allowLocalAddresses = false).fetch("http://localhost:5181/manifest.json")
      }
    exception.code.assert.isEqualTo(Message.URL_NOT_VALID.code)
    verifyNoInteractions(httpClient)
  }

  @Test
  fun `allows a local manifest URL when apps allowLocalAddresses is true`() {
    assertDoesNotThrow {
      fetcher(allowLocalAddresses = true).fetch("http://localhost:5181/manifest.json")
    }
    verify(httpClient).fetchBody("http://localhost:5181/manifest.json")
  }

  @Test
  fun `accepts a valid dashboard-only manifest`() {
    val result = assertDoesNotThrow { fetcherReturning(manifestJson).fetch(MANIFEST_URL) }
    result.manifest.id.assert
      .isEqualTo("test-app")
    result.manifest.modules.projectDashboardPage!!
      .single()
      .key.assert
      .isEqualTo("home")
  }

  @Test
  fun `rejects a manifest declaring a non-dashboard module as an unsupported feature`() {
    val json =
      manifestJson.replace(
        "\"modules\": {",
        "\"modules\": {\n\"key-action\": [{\"key\": \"a\", \"type\": \"link\"}],",
      )
    val exception = assertThrows<BadRequestException> { fetcherReturning(json).fetch(MANIFEST_URL) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
    exception.params!!
      .first()
      .assert
      .isEqualTo("unsupported manifest features: key-action")
  }

  @Test
  fun `rejects a manifest declaring a top-level decoratorsUrl as an unsupported feature`() {
    val json = manifestJson.replace("\"scopes\":", "\"decoratorsUrl\": \"https://app.example.com/d\",\n\"scopes\":")
    val exception = assertThrows<BadRequestException> { fetcherReturning(json).fetch(MANIFEST_URL) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
    exception.params!!
      .first()
      .assert
      .isEqualTo("unsupported manifest features: decoratorsUrl")
  }

  @Test
  fun `rejects a manifest declaring a top-level webhooks feature`() {
    val json =
      manifestJson.replace(
        "\"scopes\":",
        "\"webhooks\": {\"url\": \"https://app.example.com/wh\"},\n\"scopes\":",
      )
    val exception = assertThrows<BadRequestException> { fetcherReturning(json).fetch(MANIFEST_URL) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
    exception.params!!
      .first()
      .assert
      .isEqualTo("unsupported manifest features: webhooks")
  }

  @Test
  fun `rejects a manifest with no dashboard page module`() {
    val json =
      """
      {
        "id": "test-app",
        "name": "Test App",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": ["translations.view"],
        "modules": {}
      }
      """.trimIndent()
    val exception = assertThrows<BadRequestException> { fetcherReturning(json).fetch(MANIFEST_URL) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
  }

  companion object {
    private const val MANIFEST_URL = "https://example.com/manifest.json"
  }
}
