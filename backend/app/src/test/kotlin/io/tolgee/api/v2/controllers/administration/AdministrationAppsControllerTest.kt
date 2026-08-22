package io.tolgee.api.v2.controllers.administration

import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
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
 * installs on their behalf.
 */
class AdministrationAppsControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

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
    userAccount = testData.user
    appEntityId =
      objectMapper
        .readTree(
          performAuthPost(
            "/v2/organizations/${testData.organization.id}/owned-apps",
            mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
          ).andIsOk.andReturn().response.contentAsString,
        ).at("/id")
        .asLong()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a non-admin may not manage availability, an admin may`() {
    userAccount = testData.user
    performAuthGet("$appUrl/availability").andIsForbidden

    userAccount = testData.admin
    performAuthGet("$appUrl/availability").andIsOk.andAssertThatJson {
      node("availableToAll").isEqualTo(false)
      node("organizations").isArray.isEmpty()
    }
  }

  @Test
  fun `offering the app to all sets the sentinel and lets another organization install`() {
    userAccount = testData.admin
    performAuthPut("$appUrl/availability/all?available=true", null).andIsOk.andAssertThatJson {
      node("availableToAll").isEqualTo(true)
    }

    userAccount = testData.otherOwner
    performAuthPost(
      "/v2/organizations/${testData.otherOrganization.id}/apps",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk
    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
  }

  @Test
  fun `withdrawing the all-sentinel clears availableToAll`() {
    userAccount = testData.admin
    performAuthPut("$appUrl/availability/all?available=true", null).andIsOk
    performAuthPut("$appUrl/availability/all?available=false", null).andIsOk.andAssertThatJson {
      node("availableToAll").isEqualTo(false)
    }
  }

  @Test
  fun `offering the app to one organization lists exactly that organization`() {
    userAccount = testData.admin
    performAuthPut(
      "$appUrl/availability/organizations/${testData.otherOrganization.id}?available=true",
      null,
    ).andIsOk.andAssertThatJson {
      node("availableToAll").isEqualTo(false)
      node("organizations").isArray.hasSize(1)
      node("organizations[0].id").isEqualTo(testData.otherOrganization.id)
    }

    performAuthPut(
      "$appUrl/availability/organizations/${testData.otherOrganization.id}?available=false",
      null,
    ).andIsOk.andAssertThatJson {
      node("organizations").isArray.isEmpty()
    }
  }

  @Test
  fun `the installations view lists organizations that hold the app, admin only`() {
    userAccount = testData.admin
    performAuthPut("$appUrl/availability/all?available=true", null).andIsOk

    userAccount = testData.otherOwner
    performAuthPost(
      "/v2/organizations/${testData.otherOrganization.id}/apps",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk

    userAccount = testData.admin
    performAuthGet("$appUrl/installations").andIsOk.andAssertThatJson {
      node("_embedded.installingOrganizations").isArray.hasSize(2)
      node("page.totalElements").isEqualTo(2)
    }

    performAuthGet("$appUrl/installations?search=${testData.otherOrganization.slug}").andIsOk.andAssertThatJson {
      node("_embedded.installingOrganizations").isArray.hasSize(1)
      node("_embedded.installingOrganizations[0].slug").isEqualTo(testData.otherOrganization.slug)
    }

    userAccount = testData.user
    performAuthGet("$appUrl/installations").andIsForbidden
  }

  private val appUrl
    get() = "/v2/administration/apps/$appEntityId"
}
