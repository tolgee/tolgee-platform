package io.tolgee.socketio

import io.socket.client.IO
import io.socket.client.Socket
import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.WaitNotSatisfiedException
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Project
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.testng.annotations.BeforeClass
import java.net.URI

abstract class AbstractSocketIoTest : AbstractSpringTest() {
  protected lateinit var sockets: List<Socket>
  lateinit var project: Project
  lateinit var testData: BaseTestData
  lateinit var translation: Translation
  lateinit var key: Key

  var socketPorts = mutableListOf("19090")

  /**
   * Asserts that event with provided name was triggered by runnable provided in "dispatch" function
   */
  fun assertNotified(
    eventName: String,
    dispatchCallback: () -> Unit,
    assertCallback: ((value: Array<Any>) -> Unit)? = null
  ) {
    commitTransaction()
    val notified = sockets.associateWith { false }.toMutableMap()
    sockets.forEach { socket ->
      socket.on(eventName) { data ->
        notified[socket] = true
        assertCallback?.let { it(data) }
      }
    }

    dispatchCallback()

    sockets.forEach { socket ->
      try {
        waitFor(30000) {
          notified[socket] ?: false
        }
      } catch (e: WaitNotSatisfiedException) {
        e.printStackTrace()
      }
      Assertions.assertThat(notified[socket]).isTrue
        .withFailMessage("Socket IO client was not notified")
    }
  }

  fun beforePrepareSockets() {

  }

  @BeforeClass
  fun beforeClass() {
    prepareTestData()
    prepareSockets()
  }

  fun prepareSockets() {
    beforePrepareSockets()
    project = testData.projectBuilder.self
    val apiKey = apiKeyService.createApiKey(project.userOwner!!, setOf(ApiScope.TRANSLATIONS_VIEW), project).key
    sockets = socketPorts.map {
      IO.socket(URI.create("http://localhost:$it/translations?apiKey=$apiKey"))
    }
    sockets.parallelStream().forEach { socket ->
      var connected = false
      socket.on(Socket.EVENT_CONNECT) {
        connected = true
      }

      socket.on("connect_error") {
        println("connection error!")
      }

      socket.connect()

      waitFor(30000) {
        connected
      }
    }
  }

  fun prepareTestData() {
    testData = BaseTestData()
    testData.projectBuilder.apply {
      addKey {
        self {
          name = "key"
          key = this
        }
        addTranslation {
          self {
            language = testData.englishLanguage
            text = "translation"
            translation = this
          }
        }
      }
    }
    testDataService.saveTestData(testData.root)
  }
}
