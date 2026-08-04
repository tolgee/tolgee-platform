package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestPropertySource(properties = ["tolgee.apps.registration-secret=$REGISTRATION_SECRET"])
class AppSelfRegistrationControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    clearInitialUserFlag()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `registers an app with the server-wide secret and no signed-in user`() {
    selfRegister(secret = REGISTRATION_SECRET).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("clientId").isString.startsWith("tgapp_")
      node("clientSecret").isString.startsWith("tgapps_")
    }

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `repoints an already-registered app at the new manifest url without re-issuing the secret`() {
    selfRegister(secret = REGISTRATION_SECRET).andIsOk
    val installId = appInstallService.findAll(testData.organization.id).single().id

    selfRegister(secret = REGISTRATION_SECRET, manifestUrl = OTHER_MANIFEST_URL).andIsOk.andAssertThatJson {
      node("id").isEqualTo(installId)
      node("manifestUrl").isEqualTo(OTHER_MANIFEST_URL)
      node("clientSecret").isNull()
    }

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
  }

  @Test
  fun `rejects a wrong registration secret with 401`() {
    selfRegister(secret = "not-the-secret")
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_registration_secret") }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `rejects a missing registration secret with 401`() {
    selfRegister(secret = null)
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_registration_secret") }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
  }

  @Test
  fun `returns 404 for an unknown organization slug`() {
    selfRegister(secret = REGISTRATION_SECRET, organizationSlug = "no-such-org")
      .andExpect(status().isNotFound)
      .andAssertThatJson { node("code").isEqualTo("organization_not_found") }
  }

  @Test
  fun `registers a native install when no organization slug is given`() {
    makeInitialUser()

    selfRegister(secret = REGISTRATION_SECRET, organizationSlug = null).andIsOk.andAssertThatJson {
      node("appId").isEqualTo("test-app")
      node("clientId").isString.startsWith("tgapp_")
      node("clientSecret").isString.startsWith("tgapps_")
    }

    appInstallService.findAll(testData.organization.id).assert.isEmpty()
    executeInNewTransaction(platformTransactionManager) {
      val native = AppsTestFixtures.nativeInstalls(appInstallService).single()
      native.organization.assert.isNull()
      native.author.username.assert
        .isEqualTo(testData.user.username)
    }
  }

  @Test
  fun `treats a blank organization slug as a native registration`() {
    makeInitialUser()

    selfRegister(secret = REGISTRATION_SECRET, organizationSlug = "   ").andIsOk

    AppsTestFixtures.nativeInstalls(appInstallService).assert.hasSize(1)
  }

  @Test
  fun `repoints an already-registered native install without re-issuing the secret`() {
    makeInitialUser()
    selfRegister(secret = REGISTRATION_SECRET, organizationSlug = null).andIsOk
    val installId = AppsTestFixtures.nativeInstalls(appInstallService).single().id

    selfRegister(
      secret = REGISTRATION_SECRET,
      manifestUrl = OTHER_MANIFEST_URL,
      organizationSlug = null,
    ).andIsOk.andAssertThatJson {
      node("id").isEqualTo(installId)
      node("manifestUrl").isEqualTo(OTHER_MANIFEST_URL)
      node("clientSecret").isNull()
    }

    AppsTestFixtures.nativeInstalls(appInstallService).assert.hasSize(1)
  }

  @Test
  fun `rejects a native registration when the server has no initial user`() {
    selfRegister(secret = REGISTRATION_SECRET, organizationSlug = null)
      .andIsBadRequest
      .andAssertThatJson { node("code").isEqualTo("initial_user_not_found") }

    AppsTestFixtures.nativeInstalls(appInstallService).assert.isEmpty()
  }

  @Test
  fun `a native install does not collide with the same app installed by an organization`() {
    makeInitialUser()
    selfRegister(secret = REGISTRATION_SECRET).andIsOk
    selfRegister(secret = REGISTRATION_SECRET, organizationSlug = null).andIsOk

    appInstallService.findAll(testData.organization.id).assert.hasSize(1)
    AppsTestFixtures.nativeInstalls(appInstallService).assert.hasSize(1)
  }

  private fun selfRegister(
    secret: String?,
    manifestUrl: String = AppsTestFixtures.MANIFEST_URL,
    organizationSlug: String? = testData.organization.slug,
  ): ResultActions {
    logout()
    val body = mutableMapOf<String, Any?>("manifestUrl" to manifestUrl)
    organizationSlug?.let { body["organizationSlug"] = it }
    val request =
      post("/v2/public/apps/self-register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
    secret?.let { request.header("X-Tolgee-App-Registration-Secret", it) }
    return perform(request)
  }

  private fun makeInitialUser() {
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createNativeQuery("update user_account set is_initial_user = true where id = :id")
        .setParameter("id", testData.user.id)
        .executeUpdate()
    }
  }

  /**
   * `findInitialUser` ignores `deleted_at`, so a flag left behind by this class would be picked up
   * by every later test in the shared database.
   */
  private fun clearInitialUserFlag() {
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createNativeQuery("update user_account set is_initial_user = false where is_initial_user = true")
        .executeUpdate()
    }
  }

  companion object {
    private const val OTHER_MANIFEST_URL = "https://example.com/other-manifest.json"
  }
}

private const val REGISTRATION_SECRET = "test-registration-secret"
