package io.tolgee.api.v2.controllers.apps

import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration

/**
 * Minting install-scoped tokens with the app's own credentials, and the cutoff that makes revoking
 * one of those credentials take effect on tokens it already minted.
 */
class AppCredentialTokenTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData
  lateinit var appClientId: String
  lateinit var appClientSecret: String
  var appEntityId: Long = 0
  var installId: Long = 0

  @BeforeEach
  fun setup() {
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient, MANIFEST_WITH_SCOPES)

    val json = objectMapper.readTree(register(AppsTestFixtures.MANIFEST_URL))
    installId = json.get("installId").asLong()
    appEntityId = json.get("id").asLong()
    appClientId = json.get("clientId").asText()
    appClientSecret = json.get("clientSecret").asText()

    // The install must be enabled somewhere for a minted token to reach any data.
    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `mints an install token from the app's own credentials`() {
    tokenRequest(appClientId, appClientSecret, installId).andIsOk.andAssertThatJson {
      node("access_token").isString.isNotEmpty()
      node("token_type").isEqualTo("Bearer")
    }
  }

  @Test
  fun `the minted token reads the project the install is enabled for`() {
    translationsWith(mintToken()).andIsOk
  }

  @Test
  fun `refuses app credentials that name no install`() {
    tokenRequest(appClientId, appClientSecret, installId = null)
      .andExpect(status().isBadRequest)
      .andAssertThatJson { node("code").isEqualTo("app_install_id_required") }
  }

  @Test
  fun `refuses an unsupported grant type`() {
    logout()
    perform(
      post("/v2/public/apps/token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          objectMapper.writeValueAsString(
            mapOf(
              "grant_type" to "authorization_code",
              "client_id" to appClientId,
              "client_secret" to appClientSecret,
              "install_id" to installId,
            ),
          ),
        ),
    ).andExpect(status().isBadRequest)
      .andAssertThatJson { node("code").isEqualTo("app_unsupported_grant_type") }
  }

  @Test
  fun `refuses an unknown client id`() {
    tokenRequest("tgpub_does-not-exist", appClientSecret, installId)
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_credentials") }
  }

  @Test
  fun `refuses a wrong app secret`() {
    tokenRequest(appClientId, "tgpubs_not-the-secret", installId)
      .andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_credentials") }
  }

  @Test
  fun `refuses an install that belongs to a different app`() {
    AppsTestFixtures.mockManifest(appManifestHttpClient, OTHER_MANIFEST)
    val other = objectMapper.readTree(register(OTHER_MANIFEST_URL))
    val otherInstallId = other.get("installId").asLong()

    tokenRequest(appClientId, appClientSecret, otherInstallId)
      .andExpect(status().isNotFound)
      .andAssertThatJson { node("code").isEqualTo("app_install_not_found") }
  }

  @Test
  fun `discovers its installations with the app credentials alone`() {
    logout()
    perform(
      post("/v2/public/apps/installations/list")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          objectMapper.writeValueAsString(
            mapOf("client_id" to appClientId, "client_secret" to appClientSecret),
          ),
        ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.installations").isArray.hasSize(1)
      node("_embedded.installations[0].id").isEqualTo(installId)
      node("_embedded.installations[0].enabledProjects").isArray.hasSize(1)
      node("_embedded.installations[0].enabledProjects[0].id").isEqualTo(testData.project.id)
    }
  }

  @Test
  fun `discovery refuses wrong credentials`() {
    logout()
    perform(
      post("/v2/public/apps/installations/list")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          objectMapper.writeValueAsString(
            mapOf("client_id" to appClientId, "client_secret" to "tgpubs_wrong"),
          ),
        ),
    ).andExpect(status().isUnauthorized)
      .andAssertThatJson { node("code").isEqualTo("invalid_app_credentials") }
  }

  @Test
  fun `revoking a secret invalidates the tokens it already minted`() {
    val token = mintToken()
    translationsWith(token).andIsOk

    val second = issueSecret()
    // The cutoff is second-precision, matching a JWT's `iat`, so the revocation has to land in a
    // later second than the token for the token to be strictly older than it.
    currentDateProvider.move(Duration.ofSeconds(2))
    revokeSecret(firstSecretId())

    translationsWith(token).andExpect(status().isUnauthorized)
    // The replacement still mints, so the app recovers on its own.
    translationsWith(mintToken(secret = second)).andIsOk
  }

  @Test
  fun `issuing a secret invalidates nothing`() {
    val token = mintToken()
    currentDateProvider.move(Duration.ofSeconds(2))
    issueSecret()
    translationsWith(token).andIsOk
  }

  @Test
  fun `a token minted after a revocation keeps working`() {
    val second = issueSecret()
    revokeSecret(firstSecretId())
    currentDateProvider.move(Duration.ofSeconds(2))

    translationsWith(mintToken(secret = second)).andIsOk
  }

  /**
   * The "I revoked it normally, then learned it had leaked" case: a plain revoke does not stamp the
   * cutoff, so tokens the secret already minted stay live. Force-revoking the now-already-revoked
   * secret must still fire the kill switch.
   */
  @Test
  fun `force-revoking an already-revoked secret still invalidates its tokens`() {
    issueSecret()
    val leaked = firstSecretId()
    val token = mintToken()

    revokeSecretWithoutForce(leaked)
    translationsWith(token).andIsOk

    currentDateProvider.move(Duration.ofSeconds(2))
    revokeSecret(leaked)
    translationsWith(token).andExpect(status().isUnauthorized)
  }

  /** The SDK often keeps its current app token in a default Authorization header; the body-authenticated
   *  token endpoint must ignore it rather than 403 the very call that renews the token. */
  @Test
  fun `the token endpoint ignores a valid app token in the Authorization header`() {
    val token = mintToken()
    logout()
    perform(
      post("/v2/public/apps/token")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          objectMapper.writeValueAsString(
            mapOf(
              "grant_type" to "client_credentials",
              "client_id" to appClientId,
              "client_secret" to appClientSecret,
              "install_id" to installId,
            ),
          ),
        ),
    ).andIsOk
  }

  /** The cutoff is truncated to whole seconds, so a recovery token minted in the same second survives it. */
  @Test
  fun `a recovery token minted in the same second as a force-revoke survives the cutoff`() {
    currentDateProvider.forcedDate = currentDateProvider.date
    val second = issueSecret()
    revokeSecret(firstSecretId())

    translationsWith(mintToken(secret = second)).andIsOk
  }

  @Test
  fun `authenticating with app credentials stamps the secret's lastUsedAt`() {
    ownedSecretsList().andAssertThatJson { node("_embedded.appSecrets[0].lastUsedAt").isNull() }
    mintToken()
    ownedSecretsList().andAssertThatJson { node("_embedded.appSecrets[0].lastUsedAt").isNumber }
  }

  private fun register(manifestUrl: String): String =
    performAuthPost(
      "/v2/organizations/${testData.organization.id}/owned-apps",
      mapOf("manifestUrl" to manifestUrl),
    ).andIsOk.andReturn().response.contentAsString

  private fun mintToken(secret: String = appClientSecret): String {
    val response =
      tokenRequest(appClientId, secret, installId)
        .andIsOk
        .andReturn()
        .response.contentAsString
    return objectMapper.readTree(response).get("access_token").asText()
  }

  /** Issues an additional app-level secret alongside the current one and returns its plaintext. */
  private fun issueSecret(): String {
    logout()
    val response =
      perform(
        post("/v2/public/apps/app-secrets/issue")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              mapOf("client_id" to appClientId, "client_secret" to appClientSecret),
            ),
          ),
      ).andIsOk
        .andReturn()
        .response.contentAsString
    return objectMapper.readTree(response).get("secret").asText()
  }

  private fun ownedSecretsList(): ResultActions {
    loginAsUser()
    return performAuthGet("/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/secrets").andIsOk
  }

  private fun firstSecretId(): Long {
    val response = ownedSecretsList().andReturn().response.contentAsString
    val secrets = objectMapper.readTree(response).get("_embedded").get("appSecrets")
    val oldest = secrets.minByOrNull { it.get("createdAt").asLong() }
    oldest.assert.isNotNull
    return oldest!!.get("id").asLong()
  }

  private fun revokeSecret(secretId: Long) {
    loginAsUser()
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/secrets/$secretId?force=true",
    ).andIsOk
  }

  private fun revokeSecretWithoutForce(secretId: Long) {
    loginAsUser()
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/owned-apps/$appEntityId/secrets/$secretId",
    ).andIsOk
  }

  private fun loginAsUser() {
    userAccount = testData.user
  }

  private fun translationsWith(token: String): ResultActions {
    logout()
    return perform(
      get("/v2/projects/${testData.project.id}/translations")
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
    )
  }

  private fun tokenRequest(
    clientId: String,
    clientSecret: String,
    installId: Long?,
  ): ResultActions {
    logout()
    val body =
      mutableMapOf<String, Any>(
        "grant_type" to "client_credentials",
        "client_id" to clientId,
        "client_secret" to clientSecret,
      )
    installId?.let { body["install_id"] = it }
    return perform(
      post("/v2/public/apps/token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)),
    )
  }

  companion object {
    // Same host as the first app's manifest — a subdomain that resolves nowhere is rejected as an
    // invalid URL before registration is even attempted.
    private const val OTHER_MANIFEST_URL = "https://example.com/other/manifest.json"

    private val MANIFEST_WITH_SCOPES: String =
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

    private val OTHER_MANIFEST: String =
      """
      {
        "id": "other-test-app",
        "name": "Other Test App",
        "version": "0.1.0",
        "baseUrl": "https://other-app.example.com",
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "/"}
          ]
        }
      }
      """.trimIndent()
  }
}
