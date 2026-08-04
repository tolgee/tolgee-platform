package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AppsTestData
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/apps"])
class AppsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = AppsTestData().root

  @GetMapping(value = ["/manifest.json"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun manifest(): String =
    """
    {
      "id": "e2e-test-app",
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
}
