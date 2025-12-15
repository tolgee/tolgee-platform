package io.tolgee.websocket

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitFor
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.translation.Translation
import io.tolgee.service.notification.NotificationService
import io.tolgee.testing.WebsocketTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebsocketTest
abstract class AbstractWebsocketTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BaseTestData
  lateinit var translation: Translation
  lateinit var key: Key
  lateinit var anotherUser: UserAccount
  lateinit var currentUserWebsocket: WebsocketTestHelper
  lateinit var anotherUserWebsocket: WebsocketTestHelper

  @Autowired
  lateinit var notificationService: NotificationService

  @LocalServerPort
  private val port: Int? = null

  @BeforeEach
  fun beforeEach() {
    prepareTestData()
    currentUserWebsocket =
      WebsocketTestHelper(
        port,
        WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(testData.user.id)),
        testData.projectBuilder.self.id,
        testData.user.id,
      )
    anotherUserWebsocket =
      WebsocketTestHelper(
        port,
        WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(anotherUser.id)),
        testData.projectBuilder.self.id,
        anotherUser.id,
      )
  }

  @AfterEach
  fun after() {
    currentUserWebsocket.stop()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on key modification`() {
    currentUserWebsocket.listenForTranslationDataModified()
    currentUserWebsocket.assertNotified(
      {
        performProjectAuthPut("keys/${key.id}", mapOf("name" to "name edited"))
      },
    ) {
      assertThatJson(it.poll()).apply {
        node("actor.data.username").isEqualTo("test_username")
        node("data.keys[0]") {
          node("id").isValidId
          node("modifications.name") {
            node("old").isEqualTo("key")
            node("new").isEqualTo("name edited")
          }
          node("changeType").isEqualTo("MOD")
        }
        node("sourceActivity").isEqualTo("KEY_NAME_EDIT")
        node("dataCollapsed").isEqualTo(false)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on key deletion`() {
    currentUserWebsocket.listenForTranslationDataModified()
    currentUserWebsocket.assertNotified(
      {
        performProjectAuthDelete("keys/${key.id}")
      },
    ) {
      assertThatJson(it.poll()).apply {
        node("data.keys[0]") {
          node("id").isValidId
          node("modifications.name") {
            node("old").isEqualTo("key")
            node("new").isEqualTo(null)
          }
          node("changeType").isEqualTo("DEL")
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on key creation`() {
    currentUserWebsocket.listenForTranslationDataModified()
    currentUserWebsocket.assertNotified(
      {
        performProjectAuthPost("keys", mapOf("name" to "new key"))
      },
    ) {
      assertThatJson(it.poll()).apply {
        node("data.keys[0]") {
          node("id").isValidId
          node("modifications.name") {
            node("old").isEqualTo(null)
            node("new").isEqualTo("new key")
          }
          node("changeType").isEqualTo("ADD")
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on translation modification`() {
    currentUserWebsocket.listenForTranslationDataModified()
    currentUserWebsocket.assertNotified(
      {
        performProjectAuthPut(
          "translations",
          mapOf(
            "key" to key.name,
            "translations" to mapOf("en" to "haha"),
          ),
        ).andIsOk
      },
    ) {
      assertThatJson(it.poll()).apply {
        node("data.translations[0]") {
          node("id").isValidId
          node("modifications.text") {
            node("old").isEqualTo("translation")
            node("new").isEqualTo("haha")
          }
          node("relations") {
            node("key.data.name").isEqualTo("key")
            node("language.data.name").isEqualTo("English")
          }
          node("changeType").isEqualTo("MOD")
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies user on a new notification`() {
    currentUserWebsocket.listenForNotificationsChanged()
    anotherUserWebsocket.listenForNotificationsChanged()
    var newNotification: Notification? = null

    currentUserWebsocket.assertNotified(
      {
        newNotification = saveNotificationForCurrentUser()
      },
    ) {
      assertThatJson(it.poll()).apply {
        node("data.currentlyUnseenCount").isEqualTo(1)
        node("data.newNotification.id").isEqualTo(newNotification!!.id)
        node("timestamp").isNotNull
      }
    }

    anotherUserWebsocket.receivedMessages.assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies user on notifications marked as seen`() {
    val newNotification = saveNotificationForCurrentUser()

    currentUserWebsocket.listenForNotificationsChanged()
    anotherUserWebsocket.listenForNotificationsChanged()

    currentUserWebsocket.assertNotified(
      {
        executeInNewTransaction {
          notificationService.markNotificationsAsSeen(listOf(newNotification.id), testData.user.id)
        }
      },
    ) {
      assertThatJson(it.poll()).apply {
        node("data.currentlyUnseenCount").isEqualTo(0)
        node("data.newNotification").isNull()
        node("timestamp").isNotNull
      }
    }

    anotherUserWebsocket.receivedMessages.assert.isEmpty()
  }

  /**
   * The request is made by permitted user, but user without permission tries to listen, so they shell
   * not be notified
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't subscribe without permissions`() {
    currentUserWebsocket.listenForTranslationDataModified()
    anotherUserWebsocket.listenForTranslationDataModified()
    performProjectAuthPut(
      "translations",
      mapOf(
        "key" to key.name,
        "translations" to mapOf("en" to "haha"),
      ),
    ).andIsOk

    assertCurrentUserReceivedMessage()
    anotherUserWebsocket.receivedMessages.assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't subscribe as another user`() {
    currentUserWebsocket.listenForNotificationsChanged()
    val spyingUserWebsocket =
      WebsocketTestHelper(
        port,
        WebsocketTestHelper.Auth(jwtToken = jwtService.emitToken(anotherUser.id)),
        testData.projectBuilder.self.id,
        // anotherUser trying to spy on other user's websocket
        testData.user.id,
      )
    spyingUserWebsocket.listenForNotificationsChanged()
    spyingUserWebsocket.waitForForbidden()
    saveNotificationForCurrentUser()

    assertCurrentUserReceivedMessage()
    spyingUserWebsocket.receivedMessages.assert.isEmpty()
  }

  private fun assertCurrentUserReceivedMessage() {
    waitFor { currentUserWebsocket.receivedMessages.isNotEmpty() }
  }

  private fun saveNotificationForCurrentUser(): Notification {
    val notification =
      Notification().apply {
        user = testData.user
        type = NotificationType.PASSWORD_CHANGED
      }
    notificationService.notify(notification)
    return notification
  }

  private fun prepareTestData() {
    testData = BaseTestData()
    testData.root.addUserAccount {
      username = "anotherUser"
      anotherUser = this
    }
    testData.projectBuilder.apply {
      addKey {
        name = "key"
        key = this
      }.build {
        addTranslation {
          language = testData.englishLanguage
          text = "translation"
          translation = this
        }
      }
    }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }
}
