package io.tolgee.websocket

import io.tolgee.fixtures.WaitNotSatisfiedException
import io.tolgee.fixtures.waitFor
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.SimpleMessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class WebsocketTestHelper(
  val port: Int?,
  val auth: Auth,
  val projectId: Long,
  val userId: Long,
) : Logging {
  private var sessionHandler: MySessionHandler? = null
  lateinit var receivedMessages: LinkedBlockingDeque<String>

  fun listenForTranslationDataModified() {
    listen("/projects/$projectId/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}")
  }

  fun listenForBatchJobProgress() {
    listen("/projects/$projectId/${WebsocketEventType.BATCH_JOB_PROGRESS.typeName}")
  }

  fun listenForNotificationsChanged() {
    listen("/users/$userId/${WebsocketEventType.NOTIFICATIONS_CHANGED.typeName}")
  }

  private val webSocketStompClient by lazy {
    WebSocketStompClient(
      SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient()))),
    )
  }

  private var connection: StompSession? = null

  fun listen(path: String) {
    receivedMessages = LinkedBlockingDeque()

    webSocketStompClient.messageConverter = SimpleMessageConverter()
    sessionHandler = MySessionHandler(path, receivedMessages)
    connection =
      webSocketStompClient
        .connectAsync(
          "http://localhost:$port/websocket",
          WebSocketHttpHeaders(),
          getAuthHeaders(),
          sessionHandler!!,
        ).get(10, TimeUnit.SECONDS)
  }

  private fun getAuthHeaders(): StompHeaders {
    return StompHeaders().apply {
      when {
        auth.jwtToken != null -> add("jwtToken", auth.jwtToken)
        auth.apiKey != null -> add("x-api-key", auth.apiKey)
      }
    }
  }

  fun stop() {
    logger.info("Stopping websocket listener")
    try {
      sessionHandler?.subscription?.unsubscribe()
      connection?.disconnect()
    } catch (e: IllegalStateException) {
      logger.warn("Could not unsubscribe from websocket", e)
    }
    webSocketStompClient.stop()
    logger.info("Stopped websocket listener")
  }

  class MySessionHandler(
    val dest: String,
    val receivedMessages: LinkedBlockingDeque<String>,
  ) : StompSessionHandlerAdapter(),
    Logging {
    var subscription: StompSession.Subscription? = null
    var authenticationStatus: AuthenticationStatus? = null

    enum class AuthenticationStatus {
      UNAUTHENTICATED,
      FORBIDDEN,
    }

    override fun afterConnected(
      session: StompSession,
      connectedHeaders: StompHeaders,
    ) {
      logger.info("Connected to websocket")
      logger.info("Subscribing to $dest")
      subscription = session.subscribe(dest, this)
    }

    override fun handleException(
      session: StompSession,
      @Nullable command: StompCommand?,
      headers: StompHeaders,
      payload: ByteArray,
      exception: Throwable,
    ) {
      logger.warn("Stomp Error:", exception)
    }

    override fun handleTransportError(
      session: StompSession,
      exception: Throwable,
    ) {
      super.handleTransportError(session, exception)
      logger.warn("Stomp Transport Error:", exception)
    }

    override fun getPayloadType(headers: StompHeaders): Type {
      return ByteArray::class.java
    }

    override fun handleFrame(
      stompHeaders: StompHeaders,
      o: Any?,
    ) {
      handleForbidden(stompHeaders)
      handleUnauthenticated(stompHeaders)

      logger.info(
        "Handle Frame with stompHeaders: '{}' and payload: '{}'",
        stompHeaders,
        (o as? ByteArray)?.decodeToString(),
      )

      if (o !is ByteArray) {
        logger.info("Payload '{}' is not a ByteArray, not adding into received messages.", o)
        return
      }

      try {
        receivedMessages.add(o.decodeToString())
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      }
    }

    private fun handleForbidden(stompHeaders: StompHeaders) {
      if (stompHeaders.get("message")?.single() == "Forbidden") {
        authenticationStatus = AuthenticationStatus.FORBIDDEN
      }
    }

    private fun handleUnauthenticated(stompHeaders: StompHeaders) {
      if (stompHeaders.get("message")?.single() == "Unauthenticated") {
        authenticationStatus = AuthenticationStatus.UNAUTHENTICATED
      }
    }
  }

  /**
   * Asserts that event with provided name was triggered by runnable provided in "dispatch" function
   */
  fun assertNotified(
    dispatchCallback: () -> Unit,
    assertCallback: ((value: LinkedBlockingDeque<String>) -> Unit),
  ) {
    Thread.sleep(200)
    dispatchCallback()
    waitFor(3000) {
      receivedMessages.isNotEmpty()
    }
    assertCallback(receivedMessages)
    stop()
  }

  fun waitForForbidden() {
    waitForAuthenticationStatus(MySessionHandler.AuthenticationStatus.FORBIDDEN)
  }

  fun waitForUnauthenticated() {
    waitForAuthenticationStatus(MySessionHandler.AuthenticationStatus.UNAUTHENTICATED)
  }

  fun waitForAuthenticationStatus(status: MySessionHandler.AuthenticationStatus) {
    try {
      waitFor(500) {
        sessionHandler?.authenticationStatus == status
      }
    } catch (e: WaitNotSatisfiedException) {
      logger.info("Authentication status was not $status, was: ${sessionHandler?.authenticationStatus}")
      throw e
    }
  }

  data class Auth(
    val jwtToken: String? = null,
    val apiKey: String? = null,
  ) {
    init {
      if ((jwtToken == null && apiKey == null) || (jwtToken != null && apiKey != null)) {
        throw IllegalArgumentException("Either jwtToken or apiKey must be provided")
      }
    }
  }
}
