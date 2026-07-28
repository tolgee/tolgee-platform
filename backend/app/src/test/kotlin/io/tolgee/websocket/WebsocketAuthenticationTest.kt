package io.tolgee.websocket

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.addMinutes
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.Date

@SpringBootTest(
  properties = [
    "tolgee.websocket.use-redis=false",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebsocketAuthenticationTest : ProjectAuthControllerTest() {
  lateinit var testData: BaseTestData

  @Autowired
  lateinit var notificationService: NotificationService

  @LocalServerPort
  private val port: Int? = null

  @BeforeEach
  fun before() {
    testData = BaseTestData()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with JWT`() {
    saveTestData()
    testItWorksWithAuth(
      auth =
        WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with invalid JWT`() {
    saveTestData()
    testItIsUnauthenticatedWithAuth(
      auth =
        WebsocketTestHelper.Auth(jwtToken = "invalid"),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated on a user topic`() {
    saveTestData()
    val socket =
      WebsocketTestHelper(
        port,
        WebsocketTestHelper.Auth(jwtToken = "invalid"),
        testData.projectBuilder.self.id,
        testData.user.id,
      )
    socket.listenForNotificationsChanged()
    socket.waitForUnauthenticated()
  }

  // we need at least keys.view permission when using JWT
  @Test
  @ProjectJWTAuthTestMethod
  fun `forbidden with insufficient scopes on user with JWT`() {
    val user2 = testData.root.addUserAccount { username = "user2" }
    saveTestData()
    testProjectSubscribeForbidden(
      auth = WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(user2.self.id)),
      ownUserId = user2.self.id,
    )
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `works with PAK`() {
    saveTestData()
    testItWorksWithAuth(
      auth = WebsocketTestHelper.Auth(apiKey = apiKey.key),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with invalid PAK`() {
    saveTestData()
    testItIsUnauthenticatedWithAuth(
      auth = WebsocketTestHelper.Auth(apiKey = "invalid-api-key"),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with expired PAK`() {
    saveTestData()
    // Create an expired API key by manipulating date
    val expiredApiKey =
      apiKeyService.create(
        userAccount = testData.user,
        scopes = setOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW),
        project = testData.projectBuilder.self,
        expiresAt = currentDateProvider.date.addMinutes(-60).time,
      )

    testItIsUnauthenticatedWithAuth(
      auth = WebsocketTestHelper.Auth(apiKey = expiredApiKey.key),
    )
  }

  /** for api key we need at least translations.view scope */
  @Test
  @ProjectApiKeyAuthTestMethod(scopes = []) // No scopes
  fun `forbidden with insufficient scopes on PAK`() {
    saveTestData()
    testProjectSubscribeForbiddenViaControlSocket(
      auth = WebsocketTestHelper.Auth(apiKey = apiKey.key),
    )
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `api key cannot subscribe to a user topic`() {
    saveTestData()
    val keySocket = prepareSocket(WebsocketTestHelper.Auth(apiKey = apiKey.key))
    val ownerWitness =
      WebsocketTestHelper(
        port,
        WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)),
        testData.projectBuilder.self.id,
        testData.user.id,
      )
    try {
      val deniedInbox =
        keySocket.subscribeAdditional(
          "/users/${testData.user.id}/${WebsocketEventType.NOTIFICATIONS_CHANGED.typeName}",
        )
      ownerWitness.listenForNotificationsChanged()
      ownerWitness.assertNotified({ saveNotificationFor(testData.user) }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      deniedInbox.assert.isEmpty()
    } finally {
      keySocket.stop()
      ownerWitness.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `denies subscription to an unrecognized destination`() {
    saveTestData()
    val socket = prepareSocket(WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)))
    try {
      val wildcardInbox = socket.subscribeAdditional("/**")
      socket.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      wildcardInbox.assert.isEmpty()
    } finally {
      socket.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `denies an out-of-range project id without closing the connection`() {
    saveTestData()
    val socket = prepareSocket(WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)))
    try {
      val overflowInbox =
        socket.subscribeAdditional(
          "/projects/99999999999999999999999/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}",
        )
      socket.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      overflowInbox.assert.isEmpty()
    } finally {
      socket.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with PAT token`() {
    val pat =
      addPatToTestData(
        expiresAt = currentDateProvider.date.addMinutes(60),
      )
    saveTestData()
    testItWorksWithAuth(
      auth = WebsocketTestHelper.Auth(apiKey = pat.tokenWithPrefix),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with invalid PAT`() {
    saveTestData()
    testItIsUnauthenticatedWithAuth(
      auth = WebsocketTestHelper.Auth(apiKey = "tgpat_invalid"),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with expired PAT`() {
    val expiredPat =
      addPatToTestData(
        expiresAt = currentDateProvider.date.addMinutes(-60),
      )
    saveTestData()
    testItIsUnauthenticatedWithAuth(
      auth = WebsocketTestHelper.Auth(apiKey = expiredPat.tokenWithPrefix),
    )
  }

  // we need at least keys.view permission when using PAT
  @Test
  @ProjectJWTAuthTestMethod
  fun `forbidden with insufficient scopes on user with PAT`() {
    val user2 = testData.root.addUserAccount { username = "user2" }
    val pat =
      user2
        .addPat {
          description = "Test"
          this.expiresAt = currentDateProvider.date.addMinutes(60)
        }.self
    saveTestData()
    testProjectSubscribeForbidden(
      auth = WebsocketTestHelper.Auth(apiKey = pat.tokenWithPrefix),
      ownUserId = user2.self.id,
    )
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  private fun saveNotificationFor(account: UserAccount) {
    notificationService.notify(
      Notification().apply {
        user = account
        type = NotificationType.PASSWORD_CHANGED
      },
    )
  }

  fun testItWorksWithAuth(auth: WebsocketTestHelper.Auth) {
    val socket = prepareSocket(auth)
    socket.assertNotified(
      { createKey() },
      {
        assertThatJson(it.poll()).node("data").isObject
      },
    )
  }

  fun testProjectSubscribeForbidden(
    auth: WebsocketTestHelper.Auth,
    ownUserId: Long,
  ) {
    val forbiddenSocket =
      WebsocketTestHelper(port, auth, testData.projectBuilder.self.id, ownUserId)
    val deliveryWitness = prepareSocket(WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)))
    try {
      forbiddenSocket.listenForNotificationsChanged()
      val deniedInbox =
        forbiddenSocket.subscribeAdditional(
          "/projects/${testData.projectBuilder.self.id}/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}",
        )
      deliveryWitness.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      deniedInbox.assert.isEmpty()
    } finally {
      forbiddenSocket.stop()
      deliveryWitness.stop()
    }
  }

  fun testProjectSubscribeForbiddenViaControlSocket(auth: WebsocketTestHelper.Auth) {
    val forbiddenSocket = prepareSocket(auth)
    val deliveryWitness = prepareSocket(WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)))
    try {
      deliveryWitness.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      forbiddenSocket.receivedMessages.assert.isEmpty()
    } finally {
      forbiddenSocket.stop()
      deliveryWitness.stop()
    }
  }

  fun testItIsUnauthenticatedWithAuth(auth: WebsocketTestHelper.Auth) {
    val socket = prepareSocket(auth)
    socket.waitForUnauthenticated()
  }

  private fun prepareSocket(auth: WebsocketTestHelper.Auth): WebsocketTestHelper {
    val socket =
      WebsocketTestHelper(
        port,
        auth,
        testData.projectBuilder.self.id,
        testData.user.id,
      )

    socket.listenForTranslationDataModified()
    return socket
  }

  fun createKey() {
    performAuthPost("/v2/projects/${project.id}/keys", CreateKeyDto("test_key"))
      .andIsCreated
  }

  private fun addPatToTestData(expiresAt: Date): Pat {
    return testData.userAccountBuilder
      .addPat {
        description = "Test"
        this.expiresAt = expiresAt
      }.self
  }
}
