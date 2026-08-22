package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Server-admin management of a published app: its availability set and its installations view. The
 * admin makes an app available to organizations, which then self-install it - the admin never
 * installs on their behalf. Data is prepared through the service layer, not by firing requests.
 */
class AdministrationAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appAvailabilityService: AppAvailabilityService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: NativeAppsTestData
  var appEntityId: Long = 0

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    userAccount = testData.admin
    appEntityId =
      executeInNewTransaction {
        appInstallService
          .register(testData.organization, AppsTestFixtures.MANIFEST_URL, null, install = true)
          .appEntityId
      }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a non-admin may not manage availability, an admin may`() {
    userAccount = testData.user
    performAuthPut("$appUrl/available-to-all", null).andIsForbidden

    userAccount = testData.admin
    performAuthPut("$appUrl/available-to-all", null).andIsOk
    appAvailabilityService.isAvailableToAll(appEntityId).assert.isTrue()
  }

  @Test
  fun `withdrawing the all-sentinel clears availability to all`() {
    appAvailabilityService.setAvailableToAll(appEntityId)

    userAccount = testData.admin
    performAuthDelete("$appUrl/available-to-all").andIsOk
    appAvailabilityService.isAvailableToAll(appEntityId).assert.isFalse()
  }

  @Test
  fun `offering the app to one organization lists exactly that organization`() {
    userAccount = testData.admin
    performAuthPut("$appUrl/available-organizations/${testData.otherOrganization.id}", null).andIsOk

    performAuthGet("$appUrl/available-organizations").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(1)
      node("_embedded.organizations[0].id").isEqualTo(testData.otherOrganization.id)
      node("page.totalElements").isEqualTo(1)
    }
  }

  @Test
  fun `withdrawing one organization's grant empties the available-organizations list`() {
    appAvailabilityService.addAvailableOrganization(appEntityId, testData.otherOrganization.id)

    userAccount = testData.admin
    performAuthDelete("$appUrl/available-organizations/${testData.otherOrganization.id}").andIsOk
    performAuthGet("$appUrl/available-organizations").andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  fun `managing availability is refused for a non-admin`() {
    userAccount = testData.user
    performAuthGet("$appUrl/available-organizations").andIsForbidden
    performAuthDelete("$appUrl/available-to-all").andIsForbidden
    performAuthPut("$appUrl/available-organizations/${testData.otherOrganization.id}", null).andIsForbidden
    performAuthDelete("$appUrl/available-organizations/${testData.otherOrganization.id}").andIsForbidden
  }

  @Test
  fun `the installations view lists organizations that hold the app`() {
    installForOtherOrganization()

    userAccount = testData.admin
    performAuthGet("$appUrl/installations").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(2)
      node("page.totalElements").isEqualTo(2)
    }
  }

  @Test
  fun `the installations view is searchable and admin only`() {
    installForOtherOrganization()

    userAccount = testData.admin
    performAuthGet("$appUrl/installations?search=${testData.otherOrganization.slug}").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(1)
      node("_embedded.organizations[0].slug").isEqualTo(testData.otherOrganization.slug)
    }

    userAccount = testData.user
    performAuthGet("$appUrl/installations").andIsForbidden
  }

  private fun installForOtherOrganization() {
    appAvailabilityService.setAvailableToAll(appEntityId)
    executeInNewTransaction {
      appInstallService.install(testData.otherOrganization, AppsTestFixtures.MANIFEST_URL, null)
    }
  }

  private val appUrl
    get() = "/v2/administration/apps/$appEntityId"
}
