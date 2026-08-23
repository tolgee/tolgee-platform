package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsWithInstallsTestData
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppSecretService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * The publisher taking their app off the shelf. Distinct from one organization uninstalling: this
 * reaches every organization that installed the app and revokes its credentials in one operation,
 * which is what makes a compromised release recoverable without going tenant by tenant.
 */
class OwnedAppRemovalTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appSecretService: AppSecretService

  @Autowired
  lateinit var appRepository: AppRepository

  lateinit var testData: AppsWithInstallsTestData
  private var appEntityId: Long = 0
  private var otherInstallId: Long = 0

  @BeforeEach
  fun setup() {
    testData = AppsWithInstallsTestData()
    testDataService.saveTestData(testData.root)
    appEntityId = testData.app.id
    otherInstallId = testData.otherOrgInstall.id
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the owner removes the app from every organization and its credentials go with it`() {
    userAccount = testData.user
    performAuthDelete("${ownedUrl(testData.organization.id)}/$appEntityId").andIsOk

    executeInNewTransaction {
      appRepository
        .findById(appEntityId)
        .orElse(null)
        .assert
        .isNull()
      appSecretService.list(appEntityId).assert.isEmpty()
    }
    installsOfAppIn(testData.organization.id).assert.isEmpty()
    installsOfAppIn(testData.otherOrganization.id).assert.isEmpty()
  }

  @Test
  fun `an organization uninstalling only removes itself`() {
    userAccount = testData.otherOwner
    performAuthDelete("${installUrl(testData.otherOrganization.id)}/$otherInstallId").andIsOk

    installsOfAppIn(testData.otherOrganization.id).assert.isEmpty()
    installsOfAppIn(testData.organization.id).assert.hasSize(1)
    executeInNewTransaction {
      appRepository
        .findById(appEntityId)
        .orElse(null)
        .assert.isNotNull
    }
  }

  @Test
  fun `an organization that merely installed the app cannot remove it everywhere`() {
    userAccount = testData.otherOwner
    performAuthDelete("${ownedUrl(testData.otherOrganization.id)}/$appEntityId").andIsNotFound

    installsOfAppIn(testData.organization.id).assert.hasSize(1)
  }

  private fun installsOfAppIn(organizationId: Long) =
    appInstallService.findAll(organizationId).filter { it.app.id == appEntityId }

  private fun installUrl(organizationId: Long) = "/v2/organizations/$organizationId/apps"

  private fun ownedUrl(organizationId: Long) = "/v2/organizations/$organizationId/owned-apps"
}
