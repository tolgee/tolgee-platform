package io.tolgee.api.v2.controllers.apps

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
 * The one-step server-admin enrolment: an admin installs a first-party app straight into an
 * organization, bypassing the availability gate.
 */
class AppAdminInstallTest : AuthorizedControllerTest() {
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
            "/v2/organizations/${testData.organization.id}/apps/register",
            mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
          ).andIsOk.andReturn().response.contentAsString,
        ).at("/app/id")
        .asLong()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an admin installs an unavailable app into an organization`() {
    userAccount = testData.admin
    performAuthPost(installIntoUrl(), mapOf("organizationId" to testData.otherOrganization.id)).andIsOk

    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
  }

  @Test
  fun `installing into an organization that already has the app is idempotent`() {
    userAccount = testData.admin
    performAuthPost(installIntoUrl(), mapOf("organizationId" to testData.otherOrganization.id)).andIsOk
    performAuthPost(installIntoUrl(), mapOf("organizationId" to testData.otherOrganization.id)).andIsOk

    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)
  }

  @Test
  fun `an organization owner who is not a server admin may not install into an organization`() {
    userAccount = testData.user
    performAuthPost(installIntoUrl(), mapOf("organizationId" to testData.otherOrganization.id)).andIsForbidden
  }

  @Test
  fun `the installations view lists organizations that have the app, admin only`() {
    userAccount = testData.admin
    performAuthPost(installIntoUrl(), mapOf("organizationId" to testData.otherOrganization.id)).andIsOk

    performAuthGet("$ownedUrl/installations").andIsOk.andAssertThatJson {
      node("_embedded.installingOrganizations").isArray.hasSize(2)
      node("page.totalElements").isEqualTo(2)
    }

    // Search and paging happen on the server, never in the client.
    performAuthGet("$ownedUrl/installations?search=${testData.otherOrganization.slug}")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.installingOrganizations").isArray.hasSize(1)
        node("_embedded.installingOrganizations[0].slug").isEqualTo(testData.otherOrganization.slug)
      }
    performAuthGet("$ownedUrl/installations?size=1&page=1").andIsOk.andAssertThatJson {
      node("_embedded.installingOrganizations").isArray.hasSize(1)
      node("page.totalPages").isEqualTo(2)
    }

    userAccount = testData.user
    performAuthGet("$ownedUrl/installations").andIsForbidden
  }

  @Test
  fun `only a server admin can search organizations to install into`() {
    userAccount = testData.user
    performAuthGet("$ownedUrl/installable-organizations?search=").andIsForbidden

    userAccount = testData.admin
    performAuthGet("$ownedUrl/installable-organizations?search=").andIsOk
  }

  private val ownedUrl
    get() = "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId"

  private fun installIntoUrl() = "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/install-into"
}
