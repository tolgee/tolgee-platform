package io.tolgee.service.apps

import io.tolgee.model.apps.AppInstall
import org.mockito.Mockito.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable

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

  fun nativeInstalls(appInstallService: AppInstallService): List<AppInstall> {
    return appInstallService.findAllNativePaged(Pageable.ofSize(100)).content
  }

  /**
   * Native installs hang off no organization, so [io.tolgee.development.testDataBuilder.TestDataService.cleanTestData]
   * never reaches them — a leftover would keep occupying the `app_id` slot for the next test.
   */
  fun removeNativeInstalls(appInstallService: AppInstallService) {
    nativeInstalls(appInstallService).forEach {
      appInstallService.remove(organizationId = null, installId = it.id)
    }
  }
}
