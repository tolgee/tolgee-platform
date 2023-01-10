package io.tolgee.websocket

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitFor
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.concurrent.LinkedBlockingDeque

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractWebsocketTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BaseTestData
  lateinit var translation: Translation
  lateinit var key: Key
  lateinit var receivedMessages: LinkedBlockingDeque<String>
  lateinit var notPermittedUser: UserAccount

  @LocalServerPort
  private val port: Int? = null

  /**
   * Asserts that event with provided name was triggered by runnable provided in "dispatch" function
   */
  fun assertNotified(
    dispatchCallback: () -> Unit,
    assertCallback: ((value: LinkedBlockingDeque<String>) -> Unit)
  ) {
    Thread.sleep(200)
    dispatchCallback()
    waitFor(3000) {
      receivedMessages.isNotEmpty()
    }
    assertCallback(receivedMessages)
  }

  @BeforeEach
  fun beforeEach() {
    prepareTestData()
    val helper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(testData.user.id).toString(),
      testData.projectBuilder.self.id
    )
    helper.listen()
    receivedMessages = helper.receivedMessages
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on key modification`() {
    assertNotified(
      {
        performProjectAuthPut("keys/${key.id}", mapOf("name" to "name edited"))
      }
    ) {
      assertThatJson(it.poll()).apply {
        node("actor") {
          node("data") {
            node("username").isEqualTo("test_username")
          }
        }
        node("data") {
          node("keys") {
            isArray
            node("[0]") {
              node("id").isValidId
              node("modifications") {
                node("name") {
                  node("old").isEqualTo("key")
                  node("new").isEqualTo("name edited")
                }
              }
              node("changeType").isEqualTo("MOD")
            }
          }
        }
        node("sourceActivity").isEqualTo("KEY_NAME_EDIT")
        node("dataCollapsed").isEqualTo(false)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on key deletion`() {
    assertNotified(
      {
        performProjectAuthDelete("keys/${key.id}")
      }
    ) {
      assertThatJson(it.poll()).apply {
        node("data") {
          node("keys") {
            isArray
            node("[0]") {
              node("id").isValidId
              node("modifications") {
                node("name") {
                  node("old").isEqualTo("key")
                  node("new").isEqualTo(null)
                }
              }
              node("changeType").isEqualTo("DEL")
            }
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on key creation`() {
    assertNotified(
      {
        performProjectAuthPost("keys", mapOf("name" to "new key"))
      }
    ) {
      assertThatJson(it.poll()).apply {
        node("data") {
          node("keys") {
            isArray
            node("[0]") {
              node("id").isValidId
              node("modifications") {
                node("name") {
                  node("old").isEqualTo(null)
                  node("new").isEqualTo("new key")
                }
              }
              node("changeType").isEqualTo("ADD")
            }
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `notifies on translation modification`() {
    assertNotified(
      {
        performProjectAuthPut(
          "translations",
          mapOf(
            "key" to key.name,
            "translations" to mapOf("en" to "haha")
          )
        ).andIsOk
      }
    ) {
      assertThatJson(it.poll()).apply {
        node("data") {
          node("translations") {
            isArray
            node("[0]") {
              node("id").isValidId
              node("modifications") {
                node("text") {
                  node("old").isEqualTo("translation")
                  node("new").isEqualTo("haha")
                }
              }
              node("relations") {
                node("key") {
                  node("data") {
                    node("name").isEqualTo("key")
                  }
                }
                node("language") {
                  node("data") {
                    node("name").isEqualTo("English")
                  }
                }
              }
              node("changeType").isEqualTo("MOD")
            }
          }
        }
      }
    }
  }

  /**
   * The request is made by permitted user, but user without permission tries to listen, so they shell
   * not be notified
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't subscribe without permissions`() {
    val notPermittedSubscriptionHelper = WebsocketTestHelper(
      port,
      jwtTokenProvider.generateToken(notPermittedUser.id).toString(),
      testData.projectBuilder.self.id
    )
    notPermittedSubscriptionHelper.listen()
    performProjectAuthPut(
      "translations",
      mapOf(
        "key" to key.name,
        "translations" to mapOf("en" to "haha")
      )
    ).andIsOk
    Thread.sleep(1000)
    notPermittedSubscriptionHelper.receivedMessages.assert.isEmpty()

    // but authorized user received the message
    receivedMessages.assert.isNotEmpty
  }

  private fun prepareTestData() {
    testData = BaseTestData()
    testData.root.addUserAccount {
      username = "franta"
      notPermittedUser = this
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
