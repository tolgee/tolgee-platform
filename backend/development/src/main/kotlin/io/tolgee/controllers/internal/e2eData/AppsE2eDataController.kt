package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.service.apps.AppOwnerRemovalService
import io.tolgee.service.apps.AppService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/apps"])
class AppsE2eDataController(
  private val appService: AppService,
  private val appOwnerRemovalService: AppOwnerRemovalService,
) : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = AppsTestData().root

  /**
   * A registered app is keyed by its manifest id across the whole server and outlives the
   * organization that registered it, which the standard clean only soft-deletes. Left behind, it
   * makes the next test install the app instead of registering it.
   */
  override fun cleanup(): Any? {
    appService.find(APP_ID)?.let { appOwnerRemovalService.removeEverywhere(it.id) }
    return super.cleanup()
  }

  @GetMapping(value = ["/manifest.json"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun manifest(): String =
    """
    {
      "id": "$APP_ID",
      "name": "E2E Test App",
      "version": "1.0.0",
      "baseUrl": "https://e2e-app.example.com",
      "scopes": ["translations.view"],
      "modules": {
        "project-dashboard-page": [
          {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
        ]
      }
    }
    """.trimIndent()

  companion object {
    private const val APP_ID = "e2e-test-app"
  }
}
