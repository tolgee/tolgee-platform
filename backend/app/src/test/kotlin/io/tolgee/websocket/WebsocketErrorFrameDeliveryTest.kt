package io.tolgee.websocket

import io.tolgee.AbstractSpringTest
import io.tolgee.testing.WebsocketTest
import io.tolgee.websocket.WebsocketTestHelper.Auth
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration
import org.springframework.web.socket.handler.WebSocketHandlerDecorator
import org.springframework.web.socket.handler.WebSocketSessionDecorator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(
  properties = ["tolgee.websocket.use-redis=false"],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@Import(WebsocketErrorFrameDeliveryTest.HoldFlushLockConfiguration::class)
@WebsocketTest
class WebsocketErrorFrameDeliveryTest : AbstractSpringTest() {
  @LocalServerPort
  private val port: Int? = null

  @Test
  fun `the Unauthenticated ERROR frame reaches the client while CONNECTED is still being flushed`() {
    val socket = WebsocketTestHelper(port, Auth(jwtToken = "invalid"), projectId = 1, userId = 1)
    try {
      socket.listenForTranslationDataModified()
      socket.waitForUnauthenticated()
    } finally {
      socket.stop()
    }
  }

  /**
   * Keeps the session's flush lock held after CONNECTED is on the wire — the way an outbound thread
   * descheduled on a loaded CI runner does — until the rejected SUBSCRIBE has arrived and the close that
   * follows the ERROR frame is requested. Releasing it on the SUBSCRIBE alone lets the ERROR go out
   * uncontended, and the test then passes even when the frame can be lost.
   */
  @TestConfiguration
  class HoldFlushLockConfiguration : WebSocketMessageBrokerConfigurer {
    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
      registration.addDecoratorFactory { handler ->
        object : WebSocketHandlerDecorator(handler) {
          override fun afterConnectionEstablished(session: WebSocketSession) {
            super.afterConnectionEstablished(HoldAfterConnectedSession(session))
          }

          override fun handleMessage(
            session: WebSocketSession,
            message: WebSocketMessage<*>,
          ) {
            if (message.isStompFrame("SUBSCRIBE")) {
              latchesFor(session).subscribeReceived.countDown()
            }
            super.handleMessage(session, message)
          }
        }
      }
    }
  }

  private class HoldAfterConnectedSession(
    delegate: WebSocketSession,
  ) : WebSocketSessionDecorator(delegate) {
    override fun sendMessage(message: WebSocketMessage<*>) {
      super.sendMessage(message)
      if (message.isStompFrame("CONNECTED")) {
        val latches = latchesFor(this)
        latches.subscribeReceived.await(SUBSCRIBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        latches.closeRequested.await(CLOSE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      }
    }

    override fun close(status: CloseStatus) {
      latchesFor(this).closeRequested.countDown()
      super.close(status)
    }
  }

  private class SessionLatches {
    val subscribeReceived = CountDownLatch(1)
    val closeRequested = CountDownLatch(1)
  }

  companion object {
    private const val SUBSCRIBE_TIMEOUT_MS = 5000L

    private const val CLOSE_TIMEOUT_MS = 1000L

    private val latchesBySessionId = ConcurrentHashMap<String, SessionLatches>()

    private fun latchesFor(session: WebSocketSession) =
      latchesBySessionId.computeIfAbsent(session.id) { SessionLatches() }

    private fun WebSocketMessage<*>.isStompFrame(command: String) = (payload as? String)?.startsWith(command) == true
  }
}
