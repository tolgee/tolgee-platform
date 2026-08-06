package io.tolgee.api.v2.controllers.apps

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.NativeAppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.service.apps.AppInstallSecretService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * Covers the offboarding story the whole feature exists for: a developer registers an app, holds its
 * secret and leaves. The organization must be able to issue a replacement, watch the app move over,
 * revoke the old secret, and disable the leaver's account — without the app ever going down and
 * without losing the install's scopes or per-project enablements.
 */
class AppInstallSecretRotationTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appInstallService: AppInstallService

  @Autowired
  lateinit var appInstallSecretService: AppInstallSecretService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: NativeAppsTestData
  var installId: Long = 0
  lateinit var clientId: String
  lateinit var originalSecret: String

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)

    val json =
      objectMapper.readTree(
        performAuthPost(orgAppsUrl(), registerBody()).andIsOk.andReturn().response.contentAsString,
      )
    installId = json.get("id").asLong()
    clientId = json.get("clientId").asText()
    originalSecret = json.get("clientSecret").asText()

    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `phase one issues a second secret and leaves the first one working`() {
    val issued = issueSecretAsOwner()

    tokenRequest(clientId, originalSecret).andIsOk
    tokenRequest(clientId, issued.second).andIsOk
  }

  @Test
  fun `phase two rejects the revoked secret and keeps every other one`() {
    val issued = issueSecretAsOwner()
    val originalSecretId = secretIdsAsOwner().first { it != issued.first }

    userAccount = testData.user
    performAuthDelete("${orgAppsUrl()}/$installId/secrets/$originalSecretId").andIsOk

    tokenRequest(clientId, originalSecret)
      .andIsUnauthorized
      .andHasErrorMessage(Message.INVALID_APP_CREDENTIALS)
    tokenRequest(clientId, issued.second).andIsOk
  }

  @Test
  fun `records that a secret was used, so an operator can see the old one go idle`() {
    currentDateProvider.forcedDate = currentDateProvider.date
    tokenRequest(clientId, originalSecret).andIsOk

    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 5000) {
      executeInNewTransaction {
        appInstallSecretService
          .list(installId)
          .single()
          .lastUsedAt
          ?.time
          .assert
          .isEqualTo(currentDateProvider.forcedDate!!.time)
      }
    }
  }

  /** The reason rotation exists rather than delete-and-register-again. */
  @Test
  fun `a rotation keeps the install id, its scopes and its project enablements`() {
    val issued = issueSecretAsOwner()
    val originalSecretId = secretIdsAsOwner().first { it != issued.first }
    userAccount = testData.user
    performAuthDelete("${orgAppsUrl()}/$installId/secrets/$originalSecretId").andIsOk

    userAccount = testData.user
    performAuthGet(orgAppsUrl()).andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isArray.hasSize(1)
      node("_embedded.appInstalls[0].id").isEqualTo(installId)
      node("_embedded.appInstalls[0].scopes").isArray.containsExactly("translations.view")
    }

    asToken(installToken(issued.second), get(translationsUrl())).andIsOk
  }

  @Test
  fun `never discloses a secret outside the response that issued it`() {
    issueSecretAsOwner()

    userAccount = testData.user
    performAuthGet("${orgAppsUrl()}/$installId/secrets").andIsOk.andAssertThatJson {
      node("_embedded.appInstallSecrets").isArray.hasSize(2)
      node("_embedded.appInstallSecrets[0].secret").isNull()
      node("_embedded.appInstallSecrets[1].secret").isNull()
      node("_embedded.appInstallSecrets[0].prefix")
        .isString.startsWith(AppInstallService.CLIENT_SECRET_PREFIX)
    }
  }

  @Test
  fun `refuses to issue beyond the live secret cap`() {
    repeat(AppInstallSecretService.MAX_LIVE_SECRETS - 1) { issueSecretAsOwner() }

    userAccount = testData.user
    performAuthPost("${orgAppsUrl()}/$installId/secrets", null)
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_TOO_MANY_LIVE_SECRETS)
  }

  @Test
  fun `an organization member who is not an owner may not issue or revoke`() {
    val secretId = secretIdsAsOwner().single()

    userAccount = testData.member
    performAuthPost("${orgAppsUrl()}/$installId/secrets", null).andIsForbidden
    performAuthDelete("${orgAppsUrl()}/$installId/secrets/$secretId").andIsForbidden
    performAuthGet("${orgAppsUrl()}/$installId/secrets").andIsForbidden
  }

  @Test
  fun `an owner of another organization may not reach this install`() {
    userAccount = testData.otherOwner
    performAuthPost("/v2/organizations/${testData.otherOrganization.id}/apps/$installId/secrets", null)
      .andIsNotFound
  }

  @Test
  fun `an app rotates its own install unattended`() {
    val token = installToken(originalSecret)

    val issued =
      objectMapper.readTree(
        asToken(token, post(SELF_SECRETS)).andIsOk.andReturn().response.contentAsString,
      )
    val newSecret = issued.get("secret").asText()

    newSecret.assert.startsWith(AppInstallService.CLIENT_SECRET_PREFIX)
    tokenRequest(clientId, originalSecret).andIsOk
    asToken(installToken(newSecret), get(translationsUrl())).andIsOk

    val originalSecretId = secretIdsAsOwner().first { it != issued.get("id").asLong() }
    asToken(token, delete("$SELF_SECRETS/$originalSecretId")).andIsOk
    tokenRequest(clientId, originalSecret).andIsUnauthorized
  }

  @Test
  fun `an app may not revoke a secret belonging to another install`() {
    val other = registerSecondInstall()

    asToken(installToken(originalSecret), delete("$SELF_SECRETS/${other.second}"))
      .andIsNotFound
      .andHasErrorMessage(Message.APP_INSTALL_SECRET_NOT_FOUND)

    tokenRequest(other.first, other.third).andIsOk
  }

  /** An app authenticates with a secret, so revoking its only one would lock it out for good. */
  @Test
  fun `an app may not revoke its own last live secret`() {
    val secretId = secretIdsAsOwner().single()

    asToken(installToken(originalSecret), delete("$SELF_SECRETS/$secretId"))
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_CANNOT_REVOKE_LAST_SECRET)

    tokenRequest(clientId, originalSecret).andIsOk
  }

  /** An operator may, though — it is the only way to cut a leaked secret off immediately. */
  @Test
  fun `an owner may revoke the last live secret as a kill switch`() {
    val secretId = secretIdsAsOwner().single()

    userAccount = testData.user
    performAuthDelete("${orgAppsUrl()}/$installId/secrets/$secretId").andIsOk

    tokenRequest(clientId, originalSecret).andIsUnauthorized
  }

  @Test
  fun `a server admin rotates a native install and a non-admin cannot`() {
    val native = registerNativeInstall()

    userAccount = testData.user
    performAuthPost("/v2/administration/apps/${native.first}/secrets", null).andIsForbidden

    userAccount = testData.admin
    val issued =
      objectMapper.readTree(
        performAuthPost("/v2/administration/apps/${native.first}/secrets", null)
          .andIsOk.andReturn().response.contentAsString,
      )
    tokenRequest(native.second, issued.get("secret").asText()).andIsOk
    tokenRequest(native.second, native.third).andIsOk

    userAccount = testData.admin
    performAuthDelete(
      "/v2/administration/apps/${native.first}/secrets/${issued.get("id").asLong()}",
    ).andIsOk
    tokenRequest(native.second, issued.get("secret").asText()).andIsUnauthorized
  }

  /** The whole point: the app belongs to the organization, not to the person who set it up. */
  @Test
  fun `the app keeps authenticating after its author's account is disabled`() {
    userAccountService.disable(testData.user.id)

    tokenRequest(clientId, originalSecret).andIsOk
    asToken(installToken(originalSecret), get(translationsUrl())).andIsOk
  }

  @Test
  fun `an act-as request for a disabled user still fails`() {
    userAccountService.disable(testData.member.id)

    val token = installToken(originalSecret)
    asToken(
      token,
      get(translationsUrl()).header(APP_ACT_AS_USER_HEADER, testData.member.id.toString()),
    ).andIsForbidden.andHasErrorMessage(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
  }

  private fun orgAppsUrl() = "/v2/organizations/${testData.organization.id}/apps"

  private fun translationsUrl() = "/v2/projects/${testData.project.id}/translations"

  private fun registerBody() = mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL)

  /** @return the new secret's id and its plaintext. */
  private fun issueSecretAsOwner(): Pair<Long, String> {
    userAccount = testData.user
    val json =
      objectMapper.readTree(
        performAuthPost("${orgAppsUrl()}/$installId/secrets", null)
          .andIsOk.andReturn().response.contentAsString,
      )
    return json.get("id").asLong() to json.get("secret").asText()
  }

  private fun secretIdsAsOwner(): List<Long> {
    userAccount = testData.user
    val response =
      performAuthGet("${orgAppsUrl()}/$installId/secrets").andIsOk.andReturn().response.contentAsString
    return objectMapper
      .readTree(response)
      .at("/_embedded/appInstallSecrets")
      .toList()
      .filter { it.get("revokedAt").isNull }
      .map { it.get("id").asLong() }
  }

  /** @return the second install's client id, its one secret's id, and that secret's plaintext. */
  private fun registerSecondInstall(): Triple<String, Long, String> {
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, SECOND_MANIFEST)
    val json =
      objectMapper.readTree(
        performAuthPost(orgAppsUrl(), registerBody()).andIsOk.andReturn().response.contentAsString,
      )
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)
    val otherInstallId = json.get("id").asLong()
    val secrets =
      objectMapper.readTree(
        performAuthGet("${orgAppsUrl()}/$otherInstallId/secrets")
          .andIsOk.andReturn().response.contentAsString,
      )
    return Triple(
      json.get("clientId").asText(),
      secrets.at("/_embedded/appInstallSecrets/0/id").asLong(),
      json.get("clientSecret").asText(),
    )
  }

  /** @return the native install's id, client id and one-time secret. */
  private fun registerNativeInstall(): Triple<Long, String, String> {
    userAccount = testData.admin
    AppsTestFixtures.mockManifest(appManifestHttpClient, SECOND_MANIFEST)
    val json =
      objectMapper.readTree(
        performAuthPost("/v2/administration/apps", registerBody())
          .andIsOk.andReturn().response.contentAsString,
      )
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST)
    return Triple(json.get("id").asLong(), json.get("clientId").asText(), json.get("clientSecret").asText())
  }

  private fun installToken(secret: String): String {
    val response = tokenRequest(clientId, secret).andIsOk.andReturn().response.contentAsString
    return objectMapper.readTree(response).get("access_token").asText()
  }

  private fun tokenRequest(
    clientId: String,
    clientSecret: String,
  ): ResultActions {
    logout()
    return perform(
      post("/v2/public/apps/token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          objectMapper.writeValueAsString(
            mapOf(
              "grant_type" to "client_credentials",
              "client_id" to clientId,
              "client_secret" to clientSecret,
            ),
          ),
        ),
    )
  }

  private fun asToken(
    token: String,
    builder: MockHttpServletRequestBuilder,
  ): ResultActions {
    logout()
    return perform(builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
  }

  companion object {
    private const val SELF_SECRETS = "/v2/apps/self/secrets"
    private const val APP_ACT_AS_USER_HEADER = "X-Tolgee-Act-As-User-Id"

    private val MANIFEST: String =
      """
      {
        "id": "test-app",
        "name": "Test App",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": ["translations.view"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()

    private val SECOND_MANIFEST: String =
      """
      {
        "id": "second-app",
        "name": "Second App",
        "version": "0.1.0",
        "baseUrl": "https://second.example.com",
        "scopes": ["translations.view"],
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
