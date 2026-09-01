package io.tolgee.service.apps

import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

object AppsTestFixtures {
  const val MANIFEST_URL = "https://example.com/manifest.json"

  val MANIFEST: String =
    """
    {
      "id": "test-app",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()

  fun mockManifest(
    client: AppManifestHttpClient,
    json: String = MANIFEST,
  ) {
    doReturn(json).whenever(client).fetchBody(anyString())
  }

}
