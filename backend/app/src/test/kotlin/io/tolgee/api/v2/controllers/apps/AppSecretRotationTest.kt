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
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
import io.tolgee.service.apps.lifecycle.AppLifecycleHttpClient
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.JsonNode

/**
 * The answer to a leaked app credential: rotate once, not once per organization that installed the
 * app. Rotation must leave everything below the credential — installs, availability, enablements —
 * untouched.
 */
class AppSecretRotationTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appService: AppService

  @Autowired
  lateinit var appSecretService: AppSecretService

  @Autowired
  lateinit var appInstallService: AppInstallService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  @MockitoBean
  @Autowired
  lateinit var appLifecycleHttpClient: AppLifecycleHttpClient

  lateinit var testData: NativeAppsTestData
  var installId: Long = 0
  var appEntityId: Long = 0
  lateinit var appClientId: String
  lateinit var appClientSecret: String

  @BeforeEach
  fun setup() {
    testData = NativeAppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)

    val registered = register()
    installId = registered.get("id").asLong()
    appEntityId = registered.at("/app/id").asLong()
    appClientId = registered.at("/app/clientId").asText()
    appClientSecret = registered.at("/app/clientSecret").asText()

    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
  }

  @AfterEach
  fun cleanup() {
    AppsTestFixtures.removeNativeInstalls(appInstallService)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the owner issues a second secret and the first keeps working`() {
    val issued = issueAsOwner()

    issued
      .get("secret")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    appSelfList(appClientSecret).andIsOk
    appSelfList(issued.get("secret").asText()).andIsOk
  }

  @Test
  fun `a revoked app secret stops authenticating and the others keep working`() {
    val issued = issueAsOwner()
    val originalId = liveSecretIds().first { it != issued.get("id").asLong() }
    // The app moves to the new secret; only then may the old one be revoked without forcing.
    appSelfList(issued.get("secret").asText()).andIsOk

    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$originalId").andIsOk

    appSelfList(appClientSecret).andIsUnauthorized.andHasErrorMessage(Message.INVALID_APP_CREDENTIALS)
    appSelfList(issued.get("secret").asText()).andIsOk
  }

  @Test
  fun `the new secret is both returned once and pushed to the app`() {
    val issued = issueAsOwner()

    val pushed = awaitPayload("app.secret_rotated")
    pushed
      .at("/app/clientSecret")
      .asText()
      .assert
      .isEqualTo(issued.get("secret").asText())
    pushed
      .at("/app/clientId")
      .asText()
      .assert
      .isEqualTo(appClientId)

    userAccount = testData.user
    performAuthGet("${ownedAppsUrl()}/$appEntityId/secrets").andIsOk.andAssertThatJson {
      node("_embedded.appSecrets").isArray.hasSize(2)
      node("_embedded.appSecrets[0].secret").isNull()
      node("_embedded.appSecrets[1].secret").isNull()
    }
  }

  @Test
  fun `an app-level rotation leaves the install and its enablements alone`() {
    val issued = issueAsOwner()
    val originalId = liveSecretIds().first { it != issued.get("id").asLong() }
    appSelfList(issued.get("secret").asText()).andIsOk
    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$originalId").andIsOk

    // The surviving secret still mints tokens for the untouched install.
    tokenRequest(appClientId, issued.get("secret").asText()).andIsOk
    userAccount = testData.user
    performAuthGet("/v2/organizations/${testData.organization.id}/apps").andIsOk.andAssertThatJson {
      node("_embedded.appInstalls").isArray.hasSize(1)
      node("_embedded.appInstalls[0].id").isEqualTo(installId)
    }
    performAuthGet("/v2/projects/${testData.project.id}/apps").andIsOk.andAssertThatJson {
      node("_embedded.projectApps").isArray.hasSize(1)
    }
  }

  @Test
  fun `the app rotates its own app-level secret unattended`() {
    val issued =
      objectMapper.readTree(
        appSelf("issue", appClientSecret)
          .andIsOk
          .andReturn()
          .response.contentAsString,
      )

    issued
      .get("secret")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    appSelfList(appClientSecret).andIsOk
    appSelfList(issued.get("secret").asText()).andIsOk

    val originalId = liveSecretIds().first { it != issued.get("id").asLong() }
    appSelf("revoke", issued.get("secret").asText(), originalId).andIsOk
    appSelfList(appClientSecret).andIsUnauthorized
  }

  /** An app authenticates with a secret, so revoking its only one would lock it out for good. */
  @Test
  fun `the app may not revoke its own last live app-level secret`() {
    val secretId = liveSecretIds().single()

    appSelf("revoke", appClientSecret, secretId)
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_CANNOT_REVOKE_LAST_SECRET)

    appSelfList(appClientSecret).andIsOk
  }

  /** The owner may, though — it is the only way to cut a leaked credential off immediately. */
  @Test
  fun `the owner may revoke the last live app-level secret as a kill switch`() {
    val secretId = liveSecretIds().single()

    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$secretId?force=true").andIsOk

    appSelfList(appClientSecret).andIsUnauthorized
  }

  /** The guard: an owner must not revoke the old secret before the app has moved to the new one. */
  @Test
  fun `revoking a secret before the app used its replacement is refused, and forced through`() {
    val issued = issueAsOwner()
    val originalId = liveSecretIds().first { it != issued.get("id").asLong() }

    // Neither secret has been used yet, so an ordinary revoke of the original is refused.
    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$originalId")
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_SECRET_REPLACEMENT_UNUSED)
    appSelfList(appClientSecret).andIsOk

    // Force overrides the guard — the kill switch for a leaked secret.
    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$originalId?force=true").andIsOk
    appSelfList(appClientSecret).andIsUnauthorized
  }

  @Test
  fun `refuses to issue beyond the live secret cap`() {
    repeat(AppSecretService.MAX_LIVE_SECRETS - 1) { issueAsOwner() }

    userAccount = testData.user
    performAuthPost("${ownedAppsUrl()}/$appEntityId/secrets", null)
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_TOO_MANY_LIVE_SECRETS)
  }

  @Test
  fun `an organization that only installed the app reaches none of its app-level credentials`() {
    userAccount = testData.otherOwner
    performAuthPost(
      "/v2/organizations/${testData.otherOrganization.id}/apps",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk

    userAccount = testData.otherOwner
    val otherOwnedApps = "/v2/organizations/${testData.otherOrganization.id}/owned-apps"
    performAuthGet(otherOwnedApps).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
    performAuthGet("$otherOwnedApps/$appEntityId/secrets").andIsNotFound
    performAuthPost("$otherOwnedApps/$appEntityId/secrets", null).andIsNotFound
    performAuthDelete("$otherOwnedApps/$appEntityId").andIsNotFound
  }

  @Test
  fun `an organization member who is not an owner may not rotate`() {
    userAccount = testData.member
    performAuthGet("${ownedAppsUrl()}/$appEntityId/secrets").andIsForbidden
    performAuthPost("${ownedAppsUrl()}/$appEntityId/secrets", null).andIsForbidden
  }

  private fun register(): JsonNode {
    userAccount = testData.user
    val json =
      objectMapper.readTree(
        performAuthPost(
          "/v2/organizations/${testData.organization.id}/apps/register",
          mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
        ).andIsOk
          .andReturn()
          .response.contentAsString,
      )
    return json
  }

  private fun issueAsOwner(): JsonNode {
    userAccount = testData.user
    return objectMapper.readTree(
      performAuthPost("${ownedAppsUrl()}/$appEntityId/secrets", null)
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )
  }

  private fun liveSecretIds(): List<Long> {
    return executeInNewTransaction {
      appSecretService.list(appEntityId).filter { it.revokedAt == null }.map { it.id }
    }
  }

  private fun awaitPayload(eventType: String): JsonNode {
    lateinit var found: JsonNode
    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 10000) {
      val captor = argumentCaptor<String>()
      verify(appLifecycleHttpClient, atLeastOnce()).post(any(), captor.capture(), any())
      val seen = captor.allValues.map { objectMapper.readTree(it) }
      found =
        seen.lastOrNull { it.get("eventType").asText() == eventType }
          ?: throw AssertionError("no $eventType delivery yet, saw ${seen.map { it.get("eventType") }}")
    }
    return found
  }

  private fun ownedAppsUrl() = "/v2/organizations/${testData.organization.id}/owned-apps"

  private fun appSelfList(clientSecret: String): ResultActions = appSelf("list", clientSecret)

  private fun appSelf(
    action: String,
    clientSecret: String,
    secretId: Long? = null,
  ): ResultActions {
    logout()
    val body = mutableMapOf<String, Any>("client_id" to appClientId, "client_secret" to clientSecret)
    secretId?.let { body["secret_id"] = it }
    return perform(
      post("/v2/public/apps/app-secrets/$action")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)),
    )
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
              "install_id" to installId,
            ),
          ),
        ),
    )
  }
}
