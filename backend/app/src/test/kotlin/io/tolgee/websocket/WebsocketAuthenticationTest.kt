package io.tolgee.websocket

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.WebsocketAuthenticationTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationService
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.addMinutes
import io.tolgee.websocket.WebsocketTestHelper.Auth
import io.tolgee.websocket.WebsocketTestHelper.MySessionHandler.AuthenticationStatus
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.simp.user.SimpUserRegistry
import java.time.Duration
import java.time.Instant
import java.util.Date

@SpringBootTest(
  properties = [
    "tolgee.websocket.use-redis=false",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebsocketTest
class WebsocketAuthenticationTest : ProjectAuthControllerTest() {
  lateinit var testData: WebsocketAuthenticationTestData

  @Autowired
  lateinit var notificationService: NotificationService

  @Autowired
  lateinit var simpUserRegistry: SimpUserRegistry

  @LocalServerPort
  private val port: Int? = null

  @BeforeEach
  fun before() {
    testData = WebsocketAuthenticationTestData()
  }

  @AfterEach
  fun cleanUp() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with JWT`() {
    saveTestData()
    assertProjectEventsReceived(auth = ownerAuth())
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with invalid JWT`() {
    saveTestData()
    assertSocketClosedAsUnauthenticated(auth = Auth(jwtToken = "invalid"))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated on a user topic`() {
    saveTestData()
    val socket = socketFor(Auth(jwtToken = "invalid"))
    try {
      socket.listenForNotificationsChanged()
      socket.waitForUnauthenticated()
    } finally {
      socket.stop()
    }
  }

  // we need at least keys.view permission when using JWT
  @Test
  @ProjectJWTAuthTestMethod
  fun `forbidden with insufficient scopes on user with JWT`() {
    val user2 = testData.addSecondUser()
    saveTestData()
    assertProjectSubscribeForbidden(auth = Auth(jwtToken = jwtService.emitToken(user2.self.id)))
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `works with PAK`() {
    saveTestData()
    assertProjectEventsReceived(
      auth = Auth(apiKey = apiKey.key),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with invalid PAK`() {
    saveTestData()
    assertSocketClosedAsUnauthenticated(
      auth = Auth(apiKey = "invalid-api-key"),
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

    assertSocketClosedAsUnauthenticated(
      auth = Auth(apiKey = expiredApiKey.key),
    )
  }

  /** for api key we need at least translations.view scope */
  @Test
  @ProjectApiKeyAuthTestMethod(scopes = []) // No scopes
  fun `forbidden with insufficient scopes on PAK`() {
    saveTestData()
    assertProjectSubscribeForbidden(
      auth = Auth(apiKey = apiKey.key),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a key bound to another project cannot subscribe to this project's topic`() {
    saveTestData()
    // Ample scopes on purpose: anything less and the subscription is refused for the scopes, not the binding.
    val otherProject = testData.addOtherProject()
    testDataService.saveTestData(testData.root)
    val otherProjectKey =
      apiKeyService.create(
        userAccount = testData.user,
        scopes = setOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW),
        project = otherProject,
      )

    assertProjectSubscribeForbidden(
      auth = Auth(apiKey = otherProjectKey.key),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `an admin's key cannot subscribe to a project the admin has no access to`() {
    val outsideAdmin = testData.addOutsideAdmin()
    saveTestData()
    val adminKey =
      apiKeyService.create(
        userAccount = outsideAdmin,
        scopes = setOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW),
        project = testData.projectBuilder.self,
      )

    assertProjectSubscribeForbidden(
      auth = Auth(apiKey = adminKey.key),
    )
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `api key cannot subscribe to a user topic`() {
    saveTestData()
    assertUserTopicSubscribeForbidden(Auth(apiKey = apiKey.key))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `denies subscription to an unrecognized destination`() {
    saveTestData()
    assertSubscriptionSilentlyDenied("/**")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `denies a wildcard event type on the project topic`() {
    saveTestData()
    assertSubscriptionSilentlyDenied("/projects/${testData.projectBuilder.self.id}/*")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `denies an out-of-range project id without closing the connection`() {
    saveTestData()
    assertSubscriptionSilentlyDenied(
      "/projects/99999999999999999999999/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}",
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with PAT token`() {
    val pat =
      addPatToTestData(
        expiresAt = currentDateProvider.date.addMinutes(60),
      )
    saveTestData()
    assertProjectEventsReceived(
      auth = Auth(apiKey = pat.tokenWithPrefix),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `unauthenticated with invalid PAT`() {
    saveTestData()
    assertSocketClosedAsUnauthenticated(
      auth = Auth(apiKey = "tgpat_invalid"),
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
    assertSocketClosedAsUnauthenticated(
      auth = Auth(apiKey = expiredPat.tokenWithPrefix),
    )
  }

  // we need at least keys.view permission when using PAT
  @Test
  @ProjectJWTAuthTestMethod
  fun `forbidden with insufficient scopes on user with PAT`() {
    val user2 = testData.addSecondUser()
    val pat =
      user2
        .addPat {
          description = "Test"
          this.expiresAt = currentDateProvider.date.addMinutes(60)
        }.self
    saveTestData()
    assertProjectSubscribeForbidden(auth = Auth(apiKey = pat.tokenWithPrefix))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a credential the resolver refuses cannot ride in on the handshake principal`() {
    saveTestData()
    assertSocketClosedAsUnauthenticated(
      auth = Auth(jwtToken = "not-a-jwt"),
      handshakeAuthorization = "Bearer " + jwtService.emitToken(testData.user.id),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with a JWT presented as a bearer token`() {
    saveTestData()
    assertProjectEventsReceived(
      auth = Auth(bearerToken = jwtService.emitToken(testData.user.id)),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `denies a subscription to a project that does not exist without closing the connection`() {
    saveTestData()
    assertSubscriptionSilentlyDenied(
      WebsocketEventType.TRANSLATION_DATA_MODIFIED.projectDestinationFor(testData.projectBuilder.self.id + 999_999),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works over a SockJS HTTP transport`() {
    saveTestData()
    val socket = socketFor(ownerAuth(), useHttpTransport = true)
    try {
      socket.listenForTranslationDataModified()
      socket.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
    } finally {
      socket.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a refused credential cannot ride in on the handshake principal over a SockJS HTTP transport`() {
    saveTestData()
    val socket =
      socketFor(
        Auth(jwtToken = "not-a-jwt"),
        handshakeAuthorization = "Bearer " + jwtService.emitToken(testData.user.id),
        useHttpTransport = true,
      )
    try {
      socket.listenForTranslationDataModified()
      socket.waitForUnauthenticated()
    } finally {
      socket.stop()
    }
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_VIEW])
  fun `keys view alone is enough for a project topic`() {
    saveTestData()
    assertProjectEventsReceived(auth = Auth(apiKey = apiKey.key))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a user cannot subscribe to another user's topic`() {
    val user2 = testData.addSecondUser()
    saveTestData()
    assertUserTopicSubscribeForbidden(
      auth = ownerAuth(),
      topicOwner = user2.self,
      witnessAuth = Auth(jwtToken = jwtService.emitToken(user2.self.id)),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a client SEND is not relayed to subscribers of the destination`() {
    saveTestData()
    val witness = socketSubscribedToProjectTopic(ownerAuth())
    val forger = ownerSocket()
    try {
      forger.listenForTranslationDataModified()
      witness.assertNotified({
        forger.send(
          WebsocketEventType.TRANSLATION_DATA_MODIFIED.projectDestinationFor(testData.projectBuilder.self.id),
          """{"forged":true}""",
        )
        forger.subscribeBarrierAndAwaitProcessing()
        createKey()
      }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      witness.receivedMessages.assert.isEmpty()
    } finally {
      forger.stop()
      witness.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a client MESSAGE frame is not relayed to subscribers of the destination`() {
    saveTestData()
    val topic = WebsocketEventType.TRANSLATION_DATA_MODIFIED.projectDestinationFor(testData.projectBuilder.self.id)
    val witness = socketSubscribedToProjectTopic(ownerAuth())
    val forger = RawStompClient(port!!).connect("Bearer ${jwtService.emitToken(testData.user.id)}")
    try {
      witness.assertNotified({
        forger.sendFrame("MESSAGE", mapOf("destination" to topic), """{"forged":true}""")
        forger.subscribeBarrierAndAwaitProcessing(topic)
        createKey()
      }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      witness.receivedMessages.assert.isEmpty()
    } finally {
      forger.stop()
      witness.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a connection opened with the STOMP command authenticates like one opened with CONNECT`() {
    saveTestData()
    val topic = WebsocketEventType.TRANSLATION_DATA_MODIFIED.projectDestinationFor(testData.projectBuilder.self.id)
    val socket =
      RawStompClient(port!!).connect("Bearer ${jwtService.emitToken(testData.user.id)}", command = "STOMP")
    try {
      // The barrier only completes for an allowed SUBSCRIBE, which needs a CONNECT verdict on the session.
      assertDoesNotThrow { socket.subscribeBarrierAndAwaitProcessing(topic) }
    } finally {
      socket.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a refused session is not filed in the user registry under the handshake principal`() {
    saveTestData()
    val socket =
      socketFor(
        Auth(jwtToken = "not-a-jwt"),
        handshakeAuthorization = "Bearer " + jwtService.emitToken(testData.user.id),
        useHttpTransport = true,
      )
    try {
      // An unrecognized destination is denied silently, so the session stays open to be inspected.
      socket.listen("/**")
      waitFor(3000) { simpUserRegistry.users.any { it.name.startsWith("unauthenticated-") } }
      simpUserRegistry.users
        .map { it.name }
        .assert
        .doesNotContain(testData.user.username)
    } finally {
      socket.stop()
    }
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

  private fun assertProjectEventsReceived(auth: Auth) {
    val socket = socketSubscribedToProjectTopic(auth)
    try {
      socket.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
    } finally {
      socket.stop()
    }
  }

  private fun assertProjectSubscribeForbidden(auth: Auth) {
    val forbiddenSocket = socketSubscribedToProjectTopic(auth)
    val deliveryWitness = socketSubscribedToProjectTopic(ownerAuth())
    try {
      deliveryWitness.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      deliveryWitness.assertSubscribeAcknowledged()
      forbiddenSocket.assertSubscribeNotAcknowledged()
      forbiddenSocket.receivedMessages.assert.isEmpty()
      assertWasAuthenticated(forbiddenSocket)
    } finally {
      forbiddenSocket.stop()
      deliveryWitness.stop()
    }
  }

  private fun assertUserTopicSubscribeForbidden(
    auth: Auth,
    topicOwner: UserAccount = testData.user,
    witnessAuth: Auth = ownerAuth(),
  ) {
    val deniedSocket = socketSubscribedToProjectTopic(auth)
    val ownerWitness = socketFor(witnessAuth, userId = topicOwner.id)
    try {
      val denied =
        deniedSocket
          .subscribeAdditional(WebsocketEventType.NOTIFICATIONS_CHANGED.userDestinationFor(topicOwner.id))
      ownerWitness.listenForNotificationsChanged()
      ownerWitness.assertNotified({ saveNotificationFor(topicOwner) }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      broadcastOnProjectTopic()
      deniedSocket.assertNothingDelivered(denied)
      assertWasAuthenticated(deniedSocket)
    } finally {
      deniedSocket.stop()
      ownerWitness.stop()
    }
  }

  private fun assertSubscriptionSilentlyDenied(destination: String) {
    val socket = socketSubscribedToProjectTopic(ownerAuth())
    try {
      val denied = socket.subscribeAdditional(destination)
      socket.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      socket.assertNothingDelivered(denied)
    } finally {
      socket.stop()
    }
  }

  private fun broadcastOnProjectTopic() = createKey()

  private fun assertWasAuthenticated(socket: WebsocketTestHelper) {
    socket.statusTransitions.assert
      .`as`("denied, not never-authenticated")
      .doesNotContain(AuthenticationStatus.UNAUTHENTICATED)
  }

  private fun assertSocketClosedAsUnauthenticated(
    auth: Auth,
    handshakeAuthorization: String? = null,
  ) {
    val socket = socketSubscribedToProjectTopic(auth, handshakeAuthorization)
    try {
      socket.waitForUnauthenticated()
    } finally {
      socket.stop()
    }
  }

  private fun ownerAuth(): Auth = Auth(jwtToken = jwtService.emitToken(testData.user.id))

  private fun ownerSocket(): WebsocketTestHelper = socketFor(ownerAuth())

  private fun socketSubscribedToProjectTopic(
    auth: Auth,
    handshakeAuthorization: String? = null,
  ): WebsocketTestHelper = socketFor(auth, handshakeAuthorization).also { it.listenForTranslationDataModified() }

  private fun socketFor(
    auth: Auth,
    handshakeAuthorization: String? = null,
    useHttpTransport: Boolean = false,
    userId: Long = testData.user.id,
  ): WebsocketTestHelper =
    WebsocketTestHelper(
      port,
      auth,
      testData.projectBuilder.self.id,
      userId,
      handshakeAuthorization,
      useHttpTransport,
    )

  private fun createKey() {
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
