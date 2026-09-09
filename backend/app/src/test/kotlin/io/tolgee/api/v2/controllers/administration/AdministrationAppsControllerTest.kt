package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.AppsWithInstallsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Server-admin management of a published app: its availability set and its installations view. The
 * admin makes an app available to organizations, which then self-install it - the admin never
 * installs on their behalf. The app graph, including both installations, is declared through the
 * test-data DSL rather than registered through the service layer or over HTTP.
 */
class AdministrationAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appAvailabilityService: AppAvailabilityService

  @Autowired
  lateinit var appEnablementService: AppEnablementService

  lateinit var testData: AppsWithInstallsTestData
  var appEntityId: Long = 0

  @BeforeEach
  fun setup() {
    testData = AppsWithInstallsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin
    appEntityId = testData.app.id
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
    userAccount = testData.admin
    performAuthGet("$appUrl/installations").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(2)
      node("page.totalElements").isEqualTo(2)
    }
  }

  @Test
  fun `the installations view is searchable and admin only`() {
    userAccount = testData.admin
    performAuthGet("$appUrl/installations?search=${testData.otherOrganization.slug}").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(1)
      node("_embedded.organizations[0].slug").isEqualTo(testData.otherOrganization.slug)
    }

    userAccount = testData.user
    performAuthGet("$appUrl/installations").andIsForbidden
  }

  @Test
  fun `available-organizations lists the app's specific grants, admin only`() {
    userAccount = testData.admin
    performAuthGet("/v2/administration/apps/${testData.availableApp.id}/available-organizations")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.organizations").isArray.hasSize(1)
        node("_embedded.organizations[0].id").isEqualTo(testData.otherOrganization.id)
      }

    userAccount = testData.user
    performAuthGet("/v2/administration/apps/${testData.availableApp.id}/available-organizations").andIsForbidden
  }

  private val appUrl
    get() = "/v2/administration/apps/$appEntityId"
}
