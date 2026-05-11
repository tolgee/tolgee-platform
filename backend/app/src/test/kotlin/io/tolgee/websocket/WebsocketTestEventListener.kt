package io.tolgee.websocket

import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionSubscribeEvent

/**
 * Test-only timing probe. Logs when the server's STOMP handler receives a
 * SUBSCRIBE so future CI runs can show how long it actually takes between
 * the client calling `session.subscribe(...)` and the server processing it.
 *
 * Useful for evaluating whether `Thread.sleep(200)` in
 * `WebsocketTestHelper.assertNotified` is well-calibrated. Pair the logged
 * timestamp with `Client SUBSCRIBE sent` and `assertNotified: dispatching`
 * lines from `WebsocketTestHelper`.
 */
@Component
class WebsocketTestEventListener : Logging {
  @EventListener
  fun onSubscribe(event: SessionSubscribeEvent) {
    val accessor = StompHeaderAccessor.wrap(event.message)
    val correlationId = accessor.getNativeHeader(WebsocketTestSubscribeSync.CORRELATION_HEADER)?.firstOrNull()
    logger.debug(
      "Server received SUBSCRIBE (sessionId={}, dest={}, correlationId={}, t={}ms)",
      accessor.sessionId,
      accessor.destination,
      correlationId,
      System.currentTimeMillis(),
    )
    if (correlationId != null) {
      WebsocketTestSubscribeSync.notifySubscribed(correlationId)
    }
  }
}
