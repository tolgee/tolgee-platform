package io.tolgee.websocket

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

class WebsocketTestHelper(val port: Int?, val jwtToken: String, val projectId: Long) {
  lateinit var receivedMessages: LinkedBlockingDeque<String>

  fun listenForTranslationDataModified() {
    listen("/projects/$projectId/${WebsocketEventType.TRANSLATION_DATA_MODIFIED.typeName}")
  }

  fun listenForBatchJobProgress() {
    listen("/projects/$projectId/${WebsocketEventType.BATCH_JOB_PROGRESS.typeName}")
  }

  fun listen(path: String) {
    receivedMessages = LinkedBlockingDeque()

    val webSocketStompClient = WebSocketStompClient(
      SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
    )

    webSocketStompClient.messageConverter = SimpleMessageConverter()

    webSocketStompClient.connect(
      "http://localhost:$port/websocket", WebSocketHttpHeaders(),
      StompHeaders().apply { add("jwtToken", jwtToken) },
      MySessionHandler(path, receivedMessages)
    ).get(10, TimeUnit.SECONDS)
  }

  private class MySessionHandler(
    val dest: String,
    val receivedMessages: LinkedBlockingDeque<String>
  ) : StompSessionHandlerAdapter(), Logging {
    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
      session.subscribe(dest, this)
    }

    override fun handleException(
      session: StompSession,
      @Nullable command: StompCommand?,
      headers: StompHeaders,
      payload: ByteArray,
      exception: Throwable
    ) {
      logger.warn("Stomp Error:", exception)
    }

    override fun handleTransportError(session: StompSession, exception: Throwable) {
      super.handleTransportError(session, exception)
      logger.warn("Stomp Transport Error:", exception)
    }

    override fun getPayloadType(headers: StompHeaders): Type {
      return ByteArray::class.java
    }

    override fun handleFrame(stompHeaders: StompHeaders, o: Any?) {
      logger.info("Handle Frame with payload: {}", (o as? ByteArray)?.decodeToString())
      try {
        receivedMessages.add((o as ByteArray).decodeToString())
      } catch (e: InterruptedException) {
        throw RuntimeException(e)
      }
    }
  }

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
}
