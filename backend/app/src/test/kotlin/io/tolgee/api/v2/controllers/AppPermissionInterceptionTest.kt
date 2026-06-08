package io.tolgee.api.v2.controllers

import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.enums.Scope
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64
import java.util.Date

/**
 * Exercises the full app-auth pipeline end-to-end:
 *  - AuthenticationFilter recognizes `Bearer <app-token>` (audience `tg.app`) and
 *    resolves install + user + per-project enablement live from the DB.
 *  - SecurityService intersects install.grantedScopes with the user's project scopes.
 *  - OrganizationAuthorizationInterceptor rejects app tokens outright.
 *
 * Tokens are minted via the real [AppTokenService]. Revocation paths (install removed,
 * enablement removed, tokensValidNotBefore bumped) take effect immediately because the
 * filter re-resolves on every request.
 */
class AppPermissionInterceptionTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appInstallRepository: AppInstallRepository

  @Autowired
  lateinit var appEnablementService: AppEnablementService

  @Autowired
  lateinit var appTokenService: AppTokenService

  @Autowired
  lateinit var keyGenerator: KeyGenerator

  lateinit var testData: AppsTestData

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `app-token call succeeds when install and user both grant the required scope`() {
    val install = registerInstall(scopes = setOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    mvc
      .perform(get(allKeysUrl()).header("Authorization", "Bearer $token"))
      .andExpect(status().isOk)
  }

  @Test
  fun `app-token call denied when install lacks the required scope`() {
    val install = registerInstall(scopes = setOf(Scope.KEYS_EDIT))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    mvc
      .perform(get(allKeysUrl()).header("Authorization", "Bearer $token"))
      .andExpect(status().isForbidden)
  }

  @Test
  fun `app-token returns 401 when install was removed mid-life`() {
    val install = registerInstall(scopes = setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    appInstallService.remove(testData.organization.id, install.id)

    mvc
      .perform(get(allKeysUrl()).header("Authorization", "Bearer $token"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `app-token returns 401 when AppEnabledForProject was removed mid-life`() {
    val install = registerInstall(scopes = setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    appEnablementService.disable(testData.project.id, install.id)

    mvc
      .perform(get(allKeysUrl()).header("Authorization", "Bearer $token"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `app-token returns 401 when issued before user's tokensValidNotBefore`() {
    val install = registerInstall(scopes = setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    val user = userAccountService.findActive(testData.user.id)!!
    user.tokensValidNotBefore = Date(currentDateProvider.date.time + 60_000)
    userAccountService.save(user)

    mvc
      .perform(get(allKeysUrl()).header("Authorization", "Bearer $token"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `organization endpoint rejects app tokens`() {
    val install = registerInstall(scopes = setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    mvc
      .perform(
        get("/v2/organizations/${testData.organization.id}/apps")
          .header("Authorization", "Bearer $token"),
      ).andExpect(status().isForbidden)
  }

  @Test
  fun `default-permissions endpoint accepts app token when install is enabled for project`() {
    val install = registerInstall(scopes = setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val token = appTokenService.mintUserContextToken(install.id, testData.user.id, testData.project.id)

    mvc
      .perform(
        get("/v2/projects/${testData.project.id}/apps")
          .header("Authorization", "Bearer $token"),
      ).andExpect(status().isOk)
  }

  @Test
  fun `malformed Bearer token returns 401`() {
    mvc
      .perform(get(allKeysUrl()).header("Authorization", "Bearer not.a.real.jwt"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `X-API-Key with tgapps_ secret succeeds when install has required scope`() {
    val (install, secret) = registerInstallWithCredentials(setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)

    mvc
      .perform(get(allKeysUrl()).header("X-API-Key", secret))
      .andExpect(status().isOk)
  }

  @Test
  fun `X-API-Key denied when install lacks required scope`() {
    val (install, secret) = registerInstallWithCredentials(setOf(Scope.KEYS_EDIT))
    appEnablementService.enable(testData.project, install.id, testData.user)

    mvc
      .perform(get(allKeysUrl()).header("X-API-Key", secret))
      .andExpect(status().isForbidden)
  }

  @Test
  fun `X-API-Key with tgapps_ secret returns 401 when install is not enabled for project`() {
    val (_, secret) = registerInstallWithCredentials(setOf(Scope.TRANSLATIONS_VIEW))
    mvc
      .perform(get(allKeysUrl()).header("X-API-Key", secret))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `X-API-Key with wrong secret returns 401`() {
    val (install, _) = registerInstallWithCredentials(setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)

    mvc
      .perform(
        get(allKeysUrl())
          .header("X-API-Key", "tgapps_${"a".repeat(40)}"),
      ).andExpect(status().isUnauthorized)
  }

  @Test
  fun `Authorization Basic with clientId clientSecret succeeds`() {
    val (install, secret) = registerInstallWithCredentials(setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val header = "Basic " + Base64.getEncoder().encodeToString("${install.clientId}:$secret".toByteArray())

    mvc
      .perform(get(allKeysUrl()).header("Authorization", header))
      .andExpect(status().isOk)
  }

  @Test
  fun `Authorization Basic with wrong secret returns 401`() {
    val (install, _) = registerInstallWithCredentials(setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)
    val header =
      "Basic " +
        Base64.getEncoder().encodeToString("${install.clientId}:tgapps_${"x".repeat(40)}".toByteArray())

    mvc
      .perform(get(allKeysUrl()).header("Authorization", header))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `acting-as header cannot impersonate a user on a non-project endpoint`() {
    val (install, secret) = registerInstallWithCredentials(setOf(Scope.TRANSLATIONS_VIEW))
    appEnablementService.enable(testData.project, install.id, testData.user)

    // The app secret authenticates fine on this non-project endpoint as the install author...
    mvc
      .perform(get("/v2/user").header("X-API-Key", secret))
      .andExpect(status().isOk)

    // ...but it must not be able to act as an arbitrary user where there is no project to bound it.
    mvc
      .perform(
        get("/v2/user")
          .header("X-API-Key", secret)
          .header("X-Tolgee-Act-As-User-Id", testData.outsider.id.toString()),
      ).andExpect(status().isForbidden)
  }

  private fun allKeysUrl() = "/v2/projects/${testData.project.id}/all-keys"

  /**
   * Creates an `AppInstall` directly via the repository, bypassing the manifest-fetch flow.
   * Tests need full control over `grantedScopes` and shouldn't depend on a mocked HTTP layer.
   */
  private fun registerInstall(scopes: Set<Scope>): AppInstall {
    val appId = "test-app-${System.nanoTime()}"
    val install =
      AppInstall().apply {
        this.organization = testData.organization
        this.author = testData.user
        this.manifestUrl = "https://example.com/manifest.json"
        this.appId = appId
        this.name = "Test App"
        this.version = "0.1.0"
        this.baseUrl = "https://app.example.com"
        this.manifestJson = manifestJsonFor(appId)
        this.grantedScopes = scopes.toMutableSet()
      }
    return appInstallRepository.save(install)
  }

  private fun registerInstallWithCredentials(scopes: Set<Scope>): Pair<AppInstall, String> {
    val install = registerInstall(scopes)
    val plaintextSecret = "tgapps_${keyGenerator.generate(256)}"
    install.clientId = "tgapp_${keyGenerator.generate(128)}"
    install.clientSecretHash = keyGenerator.hash(plaintextSecret)
    install.clientSecretPrefix = plaintextSecret.take(10)
    install.webhookSecret = "tgappw_${keyGenerator.generate(256)}"
    return appInstallRepository.save(install) to plaintextSecret
  }

  private fun manifestJsonFor(appId: String): String =
    """
    {
      "id": "$appId",
      "name": "Test App",
      "version": "0.1.0",
      "baseUrl": "https://app.example.com",
      "modules": {"project-dashboard-page": []}
    }
    """.trimIndent()
}
