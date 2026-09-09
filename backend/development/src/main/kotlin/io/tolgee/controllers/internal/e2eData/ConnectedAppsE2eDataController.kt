package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ConnectedAppsTestData
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.OAuth2Constants
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/connected-apps"])
class ConnectedAppsE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var grantRepository: OAuth2GrantRepository

  private var currentTestData: ConnectedAppsTestData? = null
  private var cliGrant: OAuth2Grant? = null

  override val testData: TestDataBuilder
    get() {
      val data = ConnectedAppsTestData(primaryUsername = USERNAME)
      currentTestData = data
      data.addConnectedGrant(clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID, projectIds = null)
      cliGrant = data.addConnectedGrant(clientId = OAuth2Constants.CLI_CLIENT_ID, projectIds = null)
      return data.root
    }

  /**
   * The CLI grant is bound to the project once its id exists: `data.project.id` reads as `0` while
   * the graph is still unsaved, since [io.tolgee.model.StandardAuditModel.id] is only assigned on
   * persist. `saveTestData` also runs the user/grant save in its own transaction and clears the
   * persistence context afterwards, so the grant is detached here - an explicit `save` is required,
   * a plain field mutation would never be flushed.
   */
  override fun afterTestDataStored(data: TestDataBuilder) {
    val projectId = currentTestData?.project?.id ?: return
    val grant = cliGrant ?: return
    grant.bindProjects(listOf(projectId))
    grantRepository.save(grant)
  }

  companion object {
    const val USERNAME = "connected-apps@tolgee.io"
  }
}
