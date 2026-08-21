package io.tolgee.unit.apps

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
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
  @Test
  fun `rejects a local manifest URL by default, without ever fetching it`() {
    val client = clientReturning(VALID_MANIFEST)
    val exception =
      assertThrows<BadRequestException> {
        fetcher(client, allowLocalAddresses = false).fetch("http://localhost:5181/manifest.json")
      }
    exception.code.assert.isEqualTo(Message.URL_NOT_VALID.code)
    verifyNoInteractions(client)
  }

  @Test
  fun `allows a local manifest URL when apps allowLocalAddresses is true`() {
    val client = clientReturning(VALID_MANIFEST)
    assertDoesNotThrow {
      fetcher(client).fetch("http://localhost:5181/manifest.json")
    }
    verify(client).fetchBody("http://localhost:5181/manifest.json")
  }

  @Test
  fun `accepts a valid dashboard-only manifest`() {
    val result = assertDoesNotThrow { fetcher(clientReturning(VALID_MANIFEST)).fetch(MANIFEST_URL) }
    result.manifest.id.assert
      .isEqualTo("test-app")
    result.scopes.assert.containsExactly(Scope.TRANSLATIONS_VIEW)
    result.manifest.modules.projectDashboardPage!!
      .single()
      .key.assert
      .isEqualTo("home")
  }

  @Test
  fun `rejects invalid manifest JSON with the parse error and its cause`() {
    val exception =
      assertThrows<BadRequestException> { fetcher(clientReturning("{not json")).fetch(MANIFEST_URL) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
    exception.cause.assert.isNotNull
  }

  @Test
  fun `runs the validator on the parsed manifest`() {
    val json = VALID_MANIFEST.replace("\"name\": \"Test App\",", "\"name\": \"\",")
    val exception =
      assertThrows<BadRequestException> { fetcher(clientReturning(json)).fetch(MANIFEST_URL) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
    exception.params!!.assert.contains("name must not be blank")
  }

  @Test
  fun `resolves the icon into the fetch result`() {
    val json = VALID_MANIFEST.replace("\"scopes\":", "\"icon\": \"/logo.svg\",\n\"scopes\":")
    val result = fetcher(clientReturning(json)).fetch(MANIFEST_URL)
    result.icon.assert.isEqualTo("https://app.example.com/logo.svg")
  }

  private fun clientReturning(json: String): AppManifestHttpClient =
    mock<AppManifestHttpClient>().apply {
      doReturn(json).whenever(this).fetchBody(anyString())
    }

  private fun fetcher(
    client: AppManifestHttpClient,
    allowLocalAddresses: Boolean = true,
    tolgeeProperties: TolgeeProperties = TolgeeProperties(),
  ): AppManifestFetcher =
    AppManifestFetcher(
      client,
      jacksonObjectMapper(),
      UrlSecurity(InternalProperties()),
      AppsProperties().apply { this.allowLocalAddresses = allowLocalAddresses },
      tolgeeProperties,
    )

  companion object {
    private const val MANIFEST_URL = "https://example.com/manifest.json"

    private val VALID_MANIFEST =
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
  }
}
