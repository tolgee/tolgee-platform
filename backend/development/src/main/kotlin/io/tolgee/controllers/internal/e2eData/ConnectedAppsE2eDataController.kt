package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ConnectedAppsTestData
import io.tolgee.security.oauth2.OAuth2Constants

@InternalController(["internal/e2e-data/connected-apps"])
class ConnectedAppsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() {
      val data = ConnectedAppsTestData(primaryUsername = USERNAME)
      data.addConnectedGrant(clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID, projectIds = null)
      data.addConnectedGrant(clientId = OAuth2Constants.CLI_CLIENT_ID, projectIds = listOf(data.project.id))
      return data.root
    }

  companion object {
    const val USERNAME = "connected-apps@tolgee.io"
  }
}
