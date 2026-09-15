package io.tolgee.websocket

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Writes STOMP frame bytes straight onto the socket, so a test can send a command no client library would emit.
 * Uses SockJS's raw-websocket transport, which carries plain STOMP frames.
 */
class RawStompClient(
  private val port: Int,
) {
  private var session: WebSocketSession? = null
  private val connected = CountDownLatch(1)

  fun connect(
    authorization: String,
    command: String = "CONNECT",
  ): RawStompClient {
    session =
      StandardWebSocketClient()
        .execute(handler(), "ws://localhost:$port/websocket/websocket")
        .get(10, TimeUnit.SECONDS)
    sendFrame(command, mapOf("accept-version" to "1.2", "host" to "localhost", "Authorization" to authorization))
    if (!connected.await(10, TimeUnit.SECONDS)) error("No CONNECTED frame arrived")
    return this
  }

  fun sendFrame(
    command: String,
    headers: Map<String, String>,
    body: String? = null,
  ) {
    val session = session ?: error("connect() must be called before sendFrame()")
    val frame =
      buildString {
        append(command).append('\n')
        headers.forEach { (name, value) -> append(name).append(':').append(value).append('\n') }
        body?.let { append("content-length").append(':').append(it.toByteArray().size).append('\n') }
        append('\n')
        body?.let { append(it) }
        append('\u0000')
      }
    session.sendMessage(TextMessage(frame))
  }

  /**
   * [destination] must be one this session is allowed, or the wait times out — see
   * [WebsocketTestHelper.subscribeBarrierAndAwaitProcessing].
   */
  fun subscribeBarrierAndAwaitProcessing(destination: String) {
    val correlationId = UUID.randomUUID().toString()
    WebsocketTestSubscribeSync.register(correlationId)
    sendFrame(
      "SUBSCRIBE",
      mapOf(
        "id" to UUID.randomUUID().toString(),
        "destination" to destination,
        WebsocketTestSubscribeSync.CORRELATION_HEADER to correlationId,
      ),
    )
    WebsocketTestSubscribeSync.awaitSubscribedOrFail(correlationId, timeoutMs = 5000)
    WebsocketTestSubscribeSync.cleanup(correlationId)
  }

  fun stop() {
    session?.close()
    session = null
  }

  private fun handler() =
    object : TextWebSocketHandler() {
      override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
      ) {
        if (message.payload.startsWith("CONNECTED")) connected.countDown()
      }
    }
}
