package io.tolgee.api.v2.controllers.apps

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AppsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppManifestHttpClient
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppsTestFixtures
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
import tools.jackson.databind.JsonNode
import java.time.Duration

/**
 * Rolling an app's client secret: one action mints a replacement and retires the old one after a
 * grace window, so an app configured by hand can be switched over first. Rotation leaves everything
 * below the credential — installs, availability, enablements — untouched. Lifecycle delivery of the
 * new secret is a later slice; nothing here asserts it.
 */
class AppSecretRotationTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var appAvailabilityService: AppAvailabilityService

  @Autowired
  lateinit var appSecretService: AppSecretService

  @MockitoBean
  @Autowired
  lateinit var appManifestHttpClient: AppManifestHttpClient

  lateinit var testData: AppsTestData
  var installId: Long = 0
  var appEntityId: Long = 0
  lateinit var appClientId: String
  lateinit var appClientSecret: String

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = currentDateProvider.date
    testData = AppsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    AppsTestFixtures.mockManifest(appManifestHttpClient)

    val registered = register()
    installId = registered.get("installId").asLong()
    appEntityId = registered.get("id").asLong()
    appClientId = registered.get("clientId").asText()
    appClientSecret = registered.get("clientSecret").asText()

    performAuthPut("/v2/projects/${testData.project.id}/apps/$installId", null).andIsOk
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  /**
   * A rotation never cuts the old secret off on its own — Tolgee cannot know the app adopted the
   * new one.
   */
  @Test
  fun `a rotation keeps the old secret alive through the grace window`() {
    val rolled = roll(graceSeconds = ONE_DAY)

    rolled
      .at("/secret/secret")
      .asText()
      .assert
      .startsWith(AppService.APP_CLIENT_SECRET_PREFIX)
    rolled
      .at("/previousExpiresAt")
      .asLong()
      .assert
      .isGreaterThan(0)

    appSelfList(appClientSecret).andIsOk
    appSelfList(newSecretOf(rolled)).andIsOk
    activeSecretIds().assert.hasSize(2)
  }

  /** A secret already scheduled to die sooner must keep that deadline, not be pushed out by a later roll. */
  @Test
  fun `a later long-grace rotation does not extend a secret already expiring sooner`() {
    roll(graceSeconds = 60)
    roll(graceSeconds = ONE_DAY)

    currentDateProvider.move(Duration.ofSeconds(120))
    appSelfList(appClientSecret).andIsUnauthorized
    activeSecretIds().assert.hasSize(2)
  }

  @Test
  fun `the old secret stops working once the grace window passes`() {
    val rolled = roll(graceSeconds = 60)

    appSelfList(appClientSecret).andIsOk
    currentDateProvider.move(Duration.ofSeconds(120))
    appSelfList(appClientSecret).andIsUnauthorized
    appSelfList(newSecretOf(rolled)).andIsOk
    activeSecretIds().assert.hasSize(1)
  }

  /** There is no immediate-cutover path — old secrets end by expiry or by an explicit revoke. */
  @Test
  fun `rolling with zero grace is refused`() {
    userAccount = testData.user
    performAuthPost(
      "${ownedAppsUrl()}/$appEntityId/secrets/rotate",
      mapOf("graceSeconds" to 0),
    ).andIsBadRequest
  }

  @Test
  fun `a further rotation is refused once the active secret cap is reached`() {
    var newestId = 0L
    repeat(AppSecretService.MAX_LIVE_SECRETS - 1) {
      newestId = newIdOf(roll(graceSeconds = ONE_DAY))
    }
    activeSecretIds().assert.hasSize(AppSecretService.MAX_LIVE_SECRETS)

    userAccount = testData.user
    performAuthPost(
      "${ownedAppsUrl()}/$appEntityId/secrets/rotate",
      mapOf("graceSeconds" to ONE_DAY),
    ).andIsBadRequest.andHasErrorMessage(Message.APP_TOO_MANY_LIVE_SECRETS)

    // Revoking one of the expiring secrets frees the slot again.
    val expiringId = activeSecretIds().first { it != newestId }
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$expiringId").andIsOk
    roll(graceSeconds = ONE_DAY)
  }

  @Test
  fun `revoking during the grace window ends the old secret early`() {
    val rolled = roll(graceSeconds = ONE_DAY)
    val oldId = activeSecretIds().first { it != newIdOf(rolled) }

    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$oldId").andIsOk

    appSelfList(appClientSecret).andIsUnauthorized
    appSelfList(newSecretOf(rolled)).andIsOk
  }

  @Test
  fun `a rotation leaves the install and its enablements alone`() {
    val rolled = roll(graceSeconds = ONE_DAY)

    tokenRequest(appClientId, newSecretOf(rolled)).andIsOk
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

    val newId = issued.get("id").asLong()
    val oldId = activeSecretIds().first { it != newId }
    appSelf("revoke", issued.get("secret").asText(), oldId).andIsOk
    appSelfList(appClientSecret).andIsUnauthorized
  }

  /** An app authenticates with a secret, so revoking its only active one would lock it out for good. */
  @Test
  fun `the app may not revoke its own last active app-level secret`() {
    val secretId = activeSecretIds().single()

    appSelf("revoke", appClientSecret, secretId)
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_CANNOT_REVOKE_LAST_SECRET)

    appSelfList(appClientSecret).andIsOk
  }

  @Test
  fun `the owner cannot revoke the app's only active secret without force`() {
    val secretId = activeSecretIds().single()

    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$secretId")
      .andIsBadRequest
      .andHasErrorMessage(Message.APP_CANNOT_REVOKE_LAST_SECRET)

    appSelfList(appClientSecret).andIsOk
  }

  @Test
  fun `the owner may revoke the last active secret as a kill switch`() {
    val secretId = activeSecretIds().single()

    userAccount = testData.user
    performAuthDelete("${ownedAppsUrl()}/$appEntityId/secrets/$secretId?force=true").andIsOk

    appSelfList(appClientSecret).andIsUnauthorized
  }

  @Test
  fun `an organization that only installed the app reaches none of its app-level credentials`() {
    appAvailabilityService.setAvailableToAll(appEntityId)

    userAccount = testData.otherOwner
    performAuthPost(
      "/v2/organizations/${testData.otherOrganization.id}/apps",
      mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
    ).andIsOk

    userAccount = testData.otherOwner
    val otherOwnedApps = "/v2/organizations/${testData.otherOrganization.id}/owned-apps"
    performAuthGet("$otherOwnedApps/$appEntityId/secrets").andIsNotFound
    performAuthPost("$otherOwnedApps/$appEntityId/secrets/rotate", null).andIsNotFound
    performAuthDelete("$otherOwnedApps/$appEntityId").andIsNotFound
  }

  @Test
  fun `an organization member who is not an owner may not rotate`() {
    userAccount = testData.member
    performAuthGet("${ownedAppsUrl()}/$appEntityId/secrets").andIsForbidden
    performAuthPost("${ownedAppsUrl()}/$appEntityId/secrets/rotate", null).andIsForbidden
  }

  private fun register(): JsonNode {
    userAccount = testData.user
    return objectMapper.readTree(
      performAuthPost(
        "/v2/organizations/${testData.organization.id}/owned-apps",
        mapOf("manifestUrl" to AppsTestFixtures.MANIFEST_URL),
      ).andIsOk.andReturn().response.contentAsString,
    )
  }

  private fun roll(graceSeconds: Long): JsonNode {
    userAccount = testData.user
    return objectMapper.readTree(
      performAuthPost(
        "${ownedAppsUrl()}/$appEntityId/secrets/rotate",
        mapOf("graceSeconds" to graceSeconds),
      ).andIsOk.andReturn().response.contentAsString,
    )
  }

  private fun newSecretOf(rolled: JsonNode): String = rolled.at("/secret/secret").asText()

  private fun newIdOf(rolled: JsonNode): Long = rolled.at("/secret/id").asLong()

  private fun activeSecretIds(): List<Long> {
    return executeInNewTransaction {
      val now = currentDateProvider.date
      appSecretService
        .list(appEntityId)
        .filter { it.revokedAt == null && (it.expiresAt == null || it.expiresAt!!.after(now)) }
        .map { it.id }
    }
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

  companion object {
    const val ONE_DAY = 86_400L
  }
}
