package io.tolgee.websocket

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class ErrorFrameFlushingSessionDecoratorTest {
  private val written = CopyOnWriteArrayList<String>()
  private val sendEntered = CountDownLatch(1)
  private val releaseSend = CountDownLatch(1)

  private fun delegate(failBlockedSend: Boolean = false) =
    mock<WebSocketSession> {
      on { id } doReturn "session"
      on { isOpen } doReturn true
      on { sendMessage(any()) } doAnswer {
        val payload = (it.arguments[0] as WebSocketMessage<*>).payload.toString()
        if (payload == BLOCKED_FRAME) {
          sendEntered.countDown()
          releaseSend.await()
          if (failBlockedSend) throw IOException("client gone")
        }
        written.add(payload)
        Unit
      }
      on { close(any()) } doAnswer {
        written.add(CLOSED)
        Unit
      }
    }

  @Test
  fun `a protocol-error close writes the buffered ERROR frame first and drops frames sent after it`() {
    val session = ErrorFrameFlushingSessionDecorator(delegate(), SEND_TIME_LIMIT_MS, BUFFER_LIMIT)
    val sender = thread { session.sendMessage(TextMessage(BLOCKED_FRAME)) }
    sendEntered.await()
    session.sendMessage(TextMessage("ERROR"))

    val closer = thread { session.close(CloseStatus.PROTOCOL_ERROR) }
    awaitWaiting(closer)
    session.sendMessage(TextMessage("MESSAGE after the ERROR"))
    releaseSend.countDown()
    closer.join()
    sender.join()

    written.assert.containsExactly(BLOCKED_FRAME, "ERROR", CLOSED)
  }

  @Test
  fun `a protocol-error close does not wait for frames nobody is left to send`() {
    val session =
      ErrorFrameFlushingSessionDecorator(delegate(failBlockedSend = true), SEND_TIME_LIMIT_MS, BUFFER_LIMIT)
    val sender =
      thread {
        runCatching { session.sendMessage(TextMessage(BLOCKED_FRAME)) }
      }
    sendEntered.await()
    session.sendMessage(TextMessage("ERROR"))
    releaseSend.countDown()
    sender.join()

    val closeTookMs = measureTimeMillis { session.close(CloseStatus.PROTOCOL_ERROR) }

    closeTookMs.assert.isLessThan(SEND_TIME_LIMIT_MS / 2L)
  }

  @Test
  fun `a protocol-error close stops waiting once the active send has overrun the send time limit`() {
    val session = ErrorFrameFlushingSessionDecorator(delegate(), SEND_TIME_LIMIT_MS, BUFFER_LIMIT)
    val stuckSender = thread { session.sendMessage(TextMessage(BLOCKED_FRAME)) }
    try {
      sendEntered.await()
      session.sendMessage(TextMessage("ERROR"))
      Thread.sleep(SEND_TIME_LIMIT_MS + 100L)

      val closeTookMs = measureTimeMillis { session.close(CloseStatus.PROTOCOL_ERROR) }

      closeTookMs.assert.isLessThan(SEND_TIME_LIMIT_MS / 2L)
    } finally {
      releaseSend.countDown()
      stuckSender.join()
    }
  }

  private fun awaitWaiting(thread: Thread) {
    while (thread.state != Thread.State.TIMED_WAITING && thread.state != Thread.State.WAITING) {
      Thread.onSpinWait()
    }
  }

  companion object {
    private const val SEND_TIME_LIMIT_MS = 1000
    private const val BUFFER_LIMIT = 1024 * 1024
    private const val BLOCKED_FRAME = "CONNECTED"
    private const val CLOSED = "<closed>"
  }
}
