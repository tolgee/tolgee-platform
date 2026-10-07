package io.tolgee.websocket

import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Spring writes a STOMP ERROR frame and closes with PROTOCOL_ERROR straight after it. If another thread holds
 * the flush lock at that moment the frame is only buffered, and the close makes that thread discard it — the
 * client sees a bare disconnect instead of the `Unauthenticated` ERROR the webapp stops reconnecting on.
 * Remove once https://github.com/spring-projects/spring-framework/issues/37328 is fixed.
 */
class ErrorFrameFlushingSessionDecorator(
  delegate: WebSocketSession,
  sendTimeLimit: Int,
  bufferSizeLimit: Int,
) : ConcurrentWebSocketSessionDecorator(delegate, sendTimeLimit, bufferSizeLimit) {
  private var closing = false
  private var sendsInProgress = 0
  private val sendsLock = ReentrantLock()
  private val allSendsFinished = sendsLock.newCondition()

  override fun sendMessage(message: WebSocketMessage<*>) {
    sendsLock.withLock {
      if (closing) return
      sendsInProgress++
    }
    try {
      super.sendMessage(message)
    } finally {
      sendsLock.withLock {
        if (--sendsInProgress == 0) allSendsFinished.signalAll()
      }
    }
  }

  override fun close(status: CloseStatus) {
    sendsLock.withLock { closing = true }
    if (status.equalsCode(CloseStatus.PROTOCOL_ERROR)) {
      awaitSendsInProgress()
    }
    super.close(status)
  }

  /**
   * A thread leaving `sendMessage` has flushed the buffer unless its write failed, and after a failed write
   * nothing is left to send the rest, so only sends in progress are worth waiting for.
   */
  private fun awaitSendsInProgress() {
    var remainingNanos = TimeUnit.MILLISECONDS.toNanos((sendTimeLimit - timeSinceSendStarted).coerceAtLeast(0))
    sendsLock.withLock {
      while (sendsInProgress > 0 && remainingNanos > 0) {
        try {
          remainingNanos = allSendsFinished.awaitNanos(remainingNanos)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
          return
        }
      }
    }
  }
}
