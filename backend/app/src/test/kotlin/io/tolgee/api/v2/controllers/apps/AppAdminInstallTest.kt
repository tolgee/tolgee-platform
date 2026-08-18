package io.tolgee.api.v2.controllers.apps

import com.posthog.server.PostHog
import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The one-step server-admin enrolment: an admin installs a first-party app straight into an
 * organization, bypassing the availability gate, and the install is reported to analytics so we can
 * see which organization holds the integration.
 */
class AppAdminInstallTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var postHog: PostHog

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

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
  fun `an admin installs an unavailable app into an organization and it is reported`() {
    userAccount = testData.admin
    performAuthPost(installIntoUrl(), mapOf("organizationId" to testData.otherOrganization.id)).andIsOk

    appInstallService.findAll(testData.otherOrganization.id).assert.hasSize(1)

    val params = assertPostHogEventReported(postHog, "APP_INSTALLED")
    params["organizationName"].assert.isEqualTo(testData.otherOrganization.name)
    params["appId"].assert.isEqualTo("test-app")
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

  private fun installIntoUrl() =
    "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/install-into"
}
