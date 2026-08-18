package io.tolgee.api.v2.controllers.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Self-registration authenticates with the server-configured secret, of which the configuration
 * holds only the hash. The app's own configuration names the organization it registers into; naming
 * none targets the server's initial organization.
 */
class AppSelfRegistrationControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var keyGenerator: KeyGenerator

  @Autowired
  lateinit var appsProperties: AppsProperties

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: AppsTestData
  lateinit var initialOrganization: io.tolgee.model.Organization

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    // Command-line runners do not run in tests, so no initial user exists unless the data adds one.
    testData.root.apply {
      val initialUserBuilder =
        addUserAccount {
          username = "apps-initial-user@test.com"
          isInitialUser = true
        }
      initialOrganization = initialUserBuilder.defaultOrganizationBuilder.self
    }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)
    appsProperties.registrationSecretHash = keyGenerator.hash(SECRET)
  }

  @AfterEach
  fun cleanup() {
    appsProperties.registrationSecretHash = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registers an app into the named organization with no signed-in user`() {
    selfRegister(secret = SECRET, organizationSlug = testData.organization.slug).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("created").isEqualTo(true)
      node("app.clientId").isString.startsWith("tgpub_")
      node("app.clientSecret").isString.startsWith("tgpubs_")
    }

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `registers into the server's initial organization when no slug is given`() {
    selfRegister(secret = SECRET, organizationSlug = null).andIsOk.andAssertThatJson {
      node("created").isEqualTo(true)
    }

    appInstallService.findAll(initialOrganization.id).assert.hasSize(1)
    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `repoints an already-registered app at the new manifest url without disclosing credentials`() {
    selfRegister(secret = SECRET, organizationSlug = testData.organization.slug).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    selfRegister(
      secret = SECRET,
      organizationSlug = testData.organization.slug,
      manifestUrl = OTHER_MANIFEST_URL,
    ).andIsOk.andAssertThatJson {
      node("id").isEqualTo(installId)
      node("manifestUrl").isEqualTo(OTHER_MANIFEST_URL)
      node("created").isEqualTo(false)
      node("app.clientSecret").isNull()
    }

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `rejects a wrong secret with 401`() {
    selfRegister(secret = "not-the-secret", organizationSlug = testData.organization.slug)
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_registration_secret") }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `rejects a missing secret with 401`() {
    selfRegister(secret = null, organizationSlug = testData.organization.slug)
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_registration_secret") }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  /** No hash configured means self-registration is off — even the correct plaintext is refused. */
  @Test
  fun `rejects everything while no secret hash is configured`() {
    appsProperties.registrationSecretHash = null

    selfRegister(secret = SECRET, organizationSlug = testData.organization.slug)
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_registration_secret") }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `rejects an unknown organization slug`() {
    selfRegister(secret = SECRET, organizationSlug = "no-such-organization").andIsNotFound

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  private fun selfRegister(
    secret: String?,
    organizationSlug: String?,
    manifestUrl: String = AppsTestFixtures.MANIFEST_URL,
  ): ResultActions {
    logout()
    val body = mutableMapOf<String, Any>("manifestUrl" to manifestUrl)
    organizationSlug?.let { body["organizationSlug"] = it }
    val request =
      post("/v2/public/apps/self-register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
    secret?.let { request.header("X-Tolgee-App-Registration-Token", it) }
    return perform(request)
  }

  companion object {
    private const val SECRET = "test-registration-secret"
    private const val OTHER_MANIFEST_URL = "https://example.com/other-manifest.json"
  }
}
