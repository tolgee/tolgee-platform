package io.tolgee.websocket

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.WebsocketAuthenticationTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationService
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.websocket.WebsocketTestHelper.Auth
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(
  properties = [
    "tolgee.websocket.use-redis=false",
    "tolgee.authentication.enabled=false",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebsocketTest
class WebsocketAuthenticationDisabledTest : ProjectAuthControllerTest() {
  lateinit var testData: WebsocketAuthenticationTestData

  @Autowired
  lateinit var notificationService: NotificationService

  @LocalServerPort
  private val port: Int? = null

  @BeforeEach
  fun before() {
    testData = WebsocketAuthenticationTestData()
    testData.makeUserInitial()
    testData.addProjectApiKey(RESOLVABLE_API_KEY)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @AfterEach
  fun cleanUp() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a socket with no credential at all still receives project events`() {
    assertProjectEventsReceived(Auth())
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a socket sending the undefined token an unguarded client would send still receives project events`() {
    assertProjectEventsReceived(Auth(bearerToken = "undefined"))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a socket sending a stale api key still receives project events`() {
    assertProjectEventsReceived(Auth(apiKey = "invalid-api-key"))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a socket sending a stale legacy jwt header still receives project events`() {
    assertProjectEventsReceived(Auth(jwtToken = "undefined"))
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a socket with no credential receives notifications on the initial user's topic`() {
    val socket = socketFor(Auth())
    try {
      socket.listenForNotificationsChanged()
      socket.assertNotified({ saveNotificationFor() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
    } finally {
      socket.stop()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a resolvable api key keeps its own identity rather than the initial user's`() {
    val deniedSocket = socketFor(Auth(apiKey = RESOLVABLE_API_KEY))
    val witness = socketFor(Auth())
    try {
      deniedSocket.listenForTranslationDataModified()
      val denied =
        deniedSocket
          .subscribeAdditional(WebsocketEventType.NOTIFICATIONS_CHANGED.userDestinationFor(testData.user.id))
      witness.listenForNotificationsChanged()
      witness.assertNotified({ saveNotificationFor() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
      createKey()
      deniedSocket.assertNothingDelivered(denied)
    } finally {
      deniedSocket.stop()
      witness.stop()
    }
  }

  private fun assertProjectEventsReceived(auth: Auth) {
    val socket = socketFor(auth)
    try {
      socket.listenForTranslationDataModified()
      socket.assertNotified({ createKey() }) {
        assertThatJson(it.poll()).node("data").isObject
      }
    } finally {
      socket.stop()
    }
  }

  private fun socketFor(auth: Auth): WebsocketTestHelper =
    WebsocketTestHelper(port, auth, testData.projectBuilder.self.id, testData.user.id)

  private fun createKey() {
    performAuthPost("/v2/projects/${project.id}/keys", CreateKeyDto("test_key")).andIsCreated
  }

  private fun saveNotificationFor() {
    notificationService.notify(
      Notification().apply {
        user = testData.user
        type = NotificationType.PASSWORD_CHANGED
      },
    )
  }

  private companion object {
    const val RESOLVABLE_API_KEY = "test_api_key_websocket_disabled_auth"
  }
}
