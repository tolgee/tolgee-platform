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
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
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
    logger.debug("Connecting websocket (userId={}, projectId={}, dest={})", userId, projectId, path)
    receivedMessages = LinkedBlockingDeque()

    // Register the latch BEFORE the connect: the connect future's completion
    // races against the test worker resuming from .get(), and ConcurrentHashMap
    // visibility was not enough — we previously saw `awaitSubscribed` find the
    // latch missing in the same millisecond `register` happened on the
    // StompSession callback thread. Generating the correlationId here and
    // registering synchronously guarantees a strict happens-before.
    val correlationId = UUID.randomUUID().toString()
    WebsocketTestSubscribeSync.register(correlationId)

    webSocketStompClient.messageConverter = SimpleMessageConverter()
    sessionHandler = MySessionHandler(path, receivedMessages, correlationId)
    connection =
      webSocketStompClient
        .connectAsync(
          "http://localhost:$port/websocket",
          WebSocketHttpHeaders(),
          getAuthHeaders(),
          sessionHandler!!,
        ).get(10, TimeUnit.SECONDS)
    logger.debug(
      "Client SUBSCRIBE sent (sessionId={}, dest={}, correlationId={}, t={}ms)",
      connection?.sessionId,
      path,
      correlationId,
      System.currentTimeMillis(),
    )
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
    val handler = sessionHandler ?: return
    val activeConnection = connection
    sessionHandler = null
    connection = null
    logger.debug("Stopping websocket listener (sessionId={})", activeConnection?.sessionId)
    try {
      handler.subscription?.unsubscribe()
      activeConnection?.disconnect()
    } catch (e: IllegalStateException) {
      logger.warn("Could not unsubscribe from websocket", e)
    } finally {
      WebsocketTestSubscribeSync.cleanup(handler.subscribeCorrelationId)
      webSocketStompClient.stop()
      logger.debug("Stopped websocket listener")
    }
  }

  class MySessionHandler(
    val dest: String,
    val receivedMessages: LinkedBlockingDeque<String>,
    /** Correlation ID used to match this session's SUBSCRIBE with its server-side event. */
    val subscribeCorrelationId: String,
  ) : StompSessionHandlerAdapter(),
    Logging {
    var subscription: StompSession.Subscription? = null

    // Append-only history of every status the session observed. The sequence
    // matters because the ERROR frame and the transport close can fire in
    // opposite orders under CI load — checking the full list (rather than
    // just the latest value) lets a test pass when the expected rejection
    // frame arrived at all, even if a CONNECTION_LOST was recorded first
    // because the close fired before the frame was processed. If the list
    // never contains the expected status, the wait fails — with the
    // transitions logged so the failure points at the lost rejection frame.
    val statusTransitions: MutableList<AuthenticationStatus> =
      CopyOnWriteArrayList()

    val authenticationStatus: AuthenticationStatus?
      get() = statusTransitions.lastOrNull()

    enum class AuthenticationStatus {
      UNAUTHENTICATED,
      FORBIDDEN,

      // The transport closed before any ERROR frame was processed. NOT a
      // valid substitute for UNAUTHENTICATED/FORBIDDEN — the wait helper
      // fails if this is the only entry in the transitions list. Recorded
      // so that the failure log shows what we did see when the expected
      // rejection frame was lost in the server's flush-before-close window.
      CONNECTION_LOST,
    }

    private fun recordStatus(newStatus: AuthenticationStatus) {
      statusTransitions.add(newStatus)
    }

    override fun afterConnected(
      session: StompSession,
      connectedHeaders: StompHeaders,
    ) {
      logger.debug("Websocket session {} connected, subscribing to {}", session.sessionId, dest)
      val subscribeHeaders =
        StompHeaders().apply {
          destination = dest
          add(WebsocketTestSubscribeSync.CORRELATION_HEADER, subscribeCorrelationId)
        }
      subscription = session.subscribe(subscribeHeaders, this)
    }

    override fun handleException(
      session: StompSession,
      @Nullable command: StompCommand?,
      headers: StompHeaders,
      payload: ByteArray,
      exception: Throwable,
    ) {
      logger.error(
        "Websocket session {} STOMP exception (command={}, headers={}, payload={}B)",
        session.sessionId,
        command,
        headers,
        payload.size,
        exception,
      )
    }

    override fun handleTransportError(
      session: StompSession,
      exception: Throwable,
    ) {
      super.handleTransportError(session, exception)
      if (statusTransitions.isEmpty() &&
        exception is org.springframework.messaging.simp.stomp.ConnectionLostException
      ) {
        recordStatus(AuthenticationStatus.CONNECTION_LOST)
      }
      logger.error(
        "Websocket session {} transport error (transitions at close: {})",
        session.sessionId,
        statusTransitions,
        exception,
      )
    }

    override fun getPayloadType(headers: StompHeaders): Type {
      return ByteArray::class.java
    }

    override fun handleFrame(
      stompHeaders: StompHeaders,
      o: Any?,
    ) {
      val messageHeader = stompHeaders.get("message")?.singleOrNull()
      logger.debug(
        "Frame received (dest={}, messageHeader={}, payloadBytes={}, allHeaders={})",
        dest,
        messageHeader,
        (o as? ByteArray)?.size,
        stompHeaders,
      )

      handleForbidden(messageHeader)
      handleUnauthenticated(messageHeader)

      if (o !is ByteArray) {
        logger.debug("Payload '{}' is not a ByteArray, not adding into received messages.", o)
        return
      }

      try {
        receivedMessages.add(o.decodeToString())
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      }
    }

    private fun handleForbidden(messageHeader: String?) {
      if (messageHeader == "Forbidden") {
        logger.debug("Authentication status -> FORBIDDEN (dest={})", dest)
        recordStatus(AuthenticationStatus.FORBIDDEN)
      }
    }

    private fun handleUnauthenticated(messageHeader: String?) {
      if (messageHeader == "Unauthenticated") {
        logger.debug("Authentication status -> UNAUTHENTICATED (dest={})", dest)
        recordStatus(AuthenticationStatus.UNAUTHENTICATED)
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
    val handler = sessionHandler ?: error("listen() must be called before assertNotified()")
    // Wait for the server's SessionSubscribeEvent for this specific subscription.
    // Replaces a fragile Thread.sleep(200) with real synchronization. Note the
    // event fires when the SUBSCRIBE hits the inbound channel; broker
    // registration follows microseconds later on the same channel pipeline. If
    // a broadcast goes missing despite the latch having counted down, that
    // gap is the suspect — the timing logs in WebsocketTestSubscribeSync
    // will make it visible.
    WebsocketTestSubscribeSync.awaitSubscribed(handler.subscribeCorrelationId, timeoutMs = 2000)
    logger.debug("assertNotified: dispatching (dest={}, t={}ms)", handler.dest, System.currentTimeMillis())
    dispatchCallback()
    waitFor(3000) {
      receivedMessages.isNotEmpty()
    }
    logger.debug(
      "assertNotified: broadcast received (dest={}, t={}ms)",
      handler.dest,
      System.currentTimeMillis(),
    )
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
      waitFor(5000) {
        sessionHandler?.statusTransitions?.contains(status) == true
      }
    } catch (e: WaitNotSatisfiedException) {
      val transitions = sessionHandler?.statusTransitions ?: emptyList<MySessionHandler.AuthenticationStatus>()
      logger.error(
        "Expected websocket authentication status {} never observed; transitions={}. " +
          "If transitions are only [CONNECTION_LOST], the server's STOMP ERROR frame " +
          "was lost in the flush-before-close window — investigate the server-side " +
          "ERROR delivery path rather than relaxing the test.",
        status,
        transitions,
      )
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
