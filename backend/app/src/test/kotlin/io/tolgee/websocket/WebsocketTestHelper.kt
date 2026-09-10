package io.tolgee.websocket

import io.tolgee.fixtures.WaitNotSatisfiedException
import io.tolgee.fixtures.waitFor
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.messaging.MessagingException
import org.springframework.messaging.converter.SimpleMessageConverter
import org.springframework.messaging.simp.stomp.ConnectionLostException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport
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
  val handshakeAuthorization: String? = null,
  val useHttpTransport: Boolean = false,
) : Logging {
  private var sessionHandler: MySessionHandler? = null
  lateinit var receivedMessages: LinkedBlockingDeque<String>

  val statusTransitions: List<MySessionHandler.AuthenticationStatus>
    get() =
      (
        sessionHandler
          ?: error("listen() must be called, and stop() not yet, before reading statusTransitions")
      ).statusTransitions

  fun listenForTranslationDataModified() {
    listen(WebsocketEventType.TRANSLATION_DATA_MODIFIED.projectDestinationFor(projectId))
  }

  fun listenForBatchJobProgress() {
    listen(WebsocketEventType.BATCH_JOB_PROGRESS.projectDestinationFor(projectId))
  }

  fun listenForNotificationsChanged() {
    listen(WebsocketEventType.NOTIFICATIONS_CHANGED.userDestinationFor(userId))
  }

  private val transports
    get() =
      when {
        useHttpTransport -> listOf(RestTemplateXhrTransport())
        else -> listOf(WebSocketTransport(StandardWebSocketClient()))
      }

  private val webSocketStompClient by lazy {
    WebSocketStompClient(SockJsClient(transports)).apply { messageConverter = SimpleMessageConverter() }
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

    sessionHandler = MySessionHandler(path, receivedMessages, correlationId)
    connection = connect(sessionHandler!!)
    logger.debug(
      "Client SUBSCRIBE sent (sessionId={}, dest={}, correlationId={}, t={}ms)",
      connection?.sessionId,
      path,
      correlationId,
      System.currentTimeMillis(),
    )
  }

  fun send(
    destination: String,
    payload: String,
  ) {
    val session = connection ?: error("listen() must be called before send()")
    session.send(destination, payload.toByteArray())
  }

  private fun connect(handler: StompSessionHandlerAdapter): StompSession =
    webSocketStompClient
      .connectAsync(
        "http://localhost:$port/websocket",
        WebSocketHttpHeaders().apply {
          handshakeAuthorization?.let { add("Authorization", it) }
        },
        getAuthHeaders(),
        handler,
      ).get(10, TimeUnit.SECONDS)

  fun subscribeAdditional(path: String): AdditionalSubscription {
    val session = connection ?: error("listen() must be called before subscribeAdditional()")
    val primary = sessionHandler ?: error("listen() must be called before subscribeAdditional()")
    WebsocketTestSubscribeSync.awaitSubscribed(primary.subscribeCorrelationId, timeoutMs = 5000)

    val inbox = LinkedBlockingDeque<String>()
    session.subscribe(
      StompHeaders().apply {
        destination = path
      },
      MySessionHandler(path, inbox, UUID.randomUUID().toString()),
    )

    val barrierInbox = subscribeBarrierAndAwaitProcessing()
    logger.debug("Additional SUBSCRIBE processed (sessionId={}, dest={})", session.sessionId, path)
    return AdditionalSubscription(inbox, barrierInbox)
  }

  /** Frames on one session are handled in order, so everything sent before this barrier is processed too. */
  fun subscribeBarrierAndAwaitProcessing(): LinkedBlockingDeque<String> {
    val session = connection ?: error("listen() must be called before subscribeBarrierAndAwaitProcessing()")
    val primary = sessionHandler ?: error("listen() must be called before subscribeBarrierAndAwaitProcessing()")
    val barrierId = UUID.randomUUID().toString()
    val barrierInbox = LinkedBlockingDeque<String>()
    WebsocketTestSubscribeSync.register(barrierId)
    session.subscribe(
      StompHeaders().apply {
        destination = primary.dest
        add(WebsocketTestSubscribeSync.CORRELATION_HEADER, barrierId)
      },
      MySessionHandler(primary.dest, barrierInbox, barrierId),
    )
    WebsocketTestSubscribeSync.awaitSubscribedOrFail(barrierId, timeoutMs = 5000)
    WebsocketTestSubscribeSync.cleanup(barrierId)
    return barrierInbox
  }

  /**
   * Only an allowed SUBSCRIBE publishes the `SessionSubscribeEvent` this reads, and it is published before the
   * broker registers the subscription. So call this only once a broadcast on the destination has been observed,
   * or a missing acknowledgement proves nothing.
   */
  fun assertSubscribeNotAcknowledged() {
    val primary = sessionHandler ?: error("listen() must be called before assertSubscribeNotAcknowledged()")
    if (!WebsocketTestSubscribeSync.wasNotified(primary.subscribeCorrelationId)) return
    throw AssertionError("The server acknowledged a SUBSCRIBE to ${primary.dest} that should have been denied")
  }

  fun assertSubscribeAcknowledged() {
    val primary = sessionHandler ?: error("listen() must be called before assertSubscribeAcknowledged()")
    if (WebsocketTestSubscribeSync.wasNotified(primary.subscribeCorrelationId)) return
    throw AssertionError("The server never acknowledged a SUBSCRIBE to ${primary.dest} that should have been allowed")
  }

  /**
   * [inbox] is the destination under test; [laterInbox] is a subscription made after it on the session's primary
   * destination, so a frame arriving there proves one addressed to [inbox] would already have been delivered.
   */
  data class AdditionalSubscription(
    val inbox: LinkedBlockingDeque<String>,
    val laterInbox: LinkedBlockingDeque<String>,
  )

  /** Asserts nothing reached [AdditionalSubscription.inbox], on the ordering [AdditionalSubscription] documents. */
  fun assertNothingDelivered(denied: AdditionalSubscription) {
    waitFor(3000) { denied.laterInbox.isNotEmpty() }
    denied.inbox.assert.isEmpty()
  }

  private fun getAuthHeaders(): StompHeaders {
    return StompHeaders().apply {
      when {
        auth.jwtToken != null -> add("jwtToken", auth.jwtToken)
        auth.apiKey != null -> add("x-api-key", auth.apiKey)
        auth.bearerToken != null -> add("Authorization", "Bearer ${auth.bearerToken}")
      }
    }
  }

  fun stop() {
    val activeConnection = connection ?: return
    val handler = sessionHandler
    sessionHandler = null
    connection = null
    logger.debug("Stopping websocket listener (sessionId={})", activeConnection.sessionId)
    try {
      handler?.subscription?.unsubscribe()
      activeConnection.disconnect()
    } catch (e: IllegalStateException) {
      logger.warn("Could not unsubscribe from websocket", e)
    } catch (e: MessagingException) {
      logger.warn("Could not unsubscribe from a closed websocket", e)
    } finally {
      handler?.let { WebsocketTestSubscribeSync.cleanup(it.subscribeCorrelationId) }
      webSocketStompClient.stop()
      logger.debug("Stopped websocket listener")
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
  }

  fun waitForUnauthenticated() {
    waitForAuthenticationStatus(MySessionHandler.AuthenticationStatus.UNAUTHENTICATED)
  }

  private fun waitForAuthenticationStatus(status: MySessionHandler.AuthenticationStatus) {
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
    val bearerToken: String? = null,
  ) {
    init {
      if (listOfNotNull(jwtToken, apiKey, bearerToken).size > 1) {
        throw IllegalArgumentException("At most one of jwtToken, apiKey or bearerToken may be provided")
      }
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

    val statusTransitions: MutableList<AuthenticationStatus> =
      CopyOnWriteArrayList()

    enum class AuthenticationStatus {
      UNAUTHENTICATED,
      CONNECTION_LOST,
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
      command: StompCommand?,
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
      if (statusTransitions.isEmpty() && exception is ConnectionLostException) {
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

      handleUnauthenticated(messageHeader)

      if (o !is ByteArray) {
        logger.debug("Payload '{}' is not a ByteArray, not adding into received messages.", o)
        return
      }

      receivedMessages.add(o.decodeToString())
    }

    private fun handleUnauthenticated(messageHeader: String?) {
      if (messageHeader == "Unauthenticated") {
        logger.debug("Authentication status -> UNAUTHENTICATED (dest={})", dest)
        recordStatus(AuthenticationStatus.UNAUTHENTICATED)
      }
    }

    private fun recordStatus(newStatus: AuthenticationStatus) {
      statusTransitions.add(newStatus)
    }
  }
}
