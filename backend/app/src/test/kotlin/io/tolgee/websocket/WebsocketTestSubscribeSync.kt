package io.tolgee.websocket

import io.tolgee.util.Logging
import io.tolgee.util.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * In-process synchronization between the test client and the server's
 * `SessionSubscribeEvent` listener. Replaces the historical
 * `Thread.sleep(200)` in `WebsocketTestHelper.assertNotified` with a real
 * sync point: the dispatch only fires after the server has acknowledged
 * the SUBSCRIBE.
 *
 * The client generates a UUID, sends it as a STOMP `x-test-correlation`
 * header on SUBSCRIBE, registers a latch under that key, then awaits the
 * latch. The server-side `WebsocketTestEventListener` reads the same
 * header from the `SessionSubscribeEvent` and counts the latch down.
 *
 * Caveat for race detection: Spring publishes `SessionSubscribeEvent`
 * BEFORE the broker handler registers the subscription (both subscribe to
 * `clientInboundChannel`, may run on different threads). So waking up on
 * the event fires very close to — but not strictly *after* — actual
 * registration. If broadcasts go missing despite the latch having
 * counted down, that gap is the suspect; the timing logs in
 * [awaitSubscribed] and [notifySubscribed] make it diagnosable.
 */
object WebsocketTestSubscribeSync : Logging {
  const val CORRELATION_HEADER = "x-test-correlation"

  private val latches = ConcurrentHashMap<String, CountDownLatch>()
  private val registeredAt = ConcurrentHashMap<String, Long>()

  fun register(correlationId: String) {
    latches[correlationId] = CountDownLatch(1)
    registeredAt[correlationId] = System.currentTimeMillis()
    logger.debug("WS subscribe latch registered (correlationId={}, t={}ms)", correlationId, System.currentTimeMillis())
  }

  fun notifySubscribed(correlationId: String) {
    val latch = latches[correlationId]
    if (latch == null) {
      logger.debug(
        "WS subscribe event for unknown correlationId={} (latch not yet registered or already cleaned up)",
        correlationId,
      )
      return
    }
    latch.countDown()
    val registered = registeredAt[correlationId]
    val elapsed = if (registered != null) "${System.currentTimeMillis() - registered}ms" else "?"
    logger.debug(
      "WS subscribe latch counted down (correlationId={}, since-register={}, t={}ms)",
      correlationId,
      elapsed,
      System.currentTimeMillis(),
    )
  }

  /**
   * Block until [notifySubscribed] is called for [correlationId] or [timeoutMs]
   * elapses. Returns the actual wait duration in ms (always logged so we can
   * compare against the prior 200 ms blanket sleep across CI runs).
   */
  fun awaitSubscribed(
    correlationId: String,
    timeoutMs: Long,
  ): Long {
    val latch = latches[correlationId]
    if (latch == null) {
      // Should not normally happen — register() runs synchronously inside
      // afterConnected, which completes before listen() returns. If we get
      // here, something race-y happened (e.g. cleanup ran out of order). Log
      // loudly with the full registry state and fall back to a short blocking
      // sleep so the test still runs.
      logger.error(
        "WS subscribe latch missing on awaitSubscribed (correlationId={}, current latches={}). " +
          "Falling back to 200 ms sleep so the test still runs; investigate the missing register/cleanup order.",
        correlationId,
        latches.keys,
      )
      Thread.sleep(200)
      return 200
    }
    val start = System.currentTimeMillis()
    val ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    val waited = System.currentTimeMillis() - start
    if (ok) {
      logger.debug("WS subscribe latch awaited successfully (correlationId={}, waited={}ms)", correlationId, waited)
    } else {
      logger.error(
        "WS subscribe latch TIMEOUT after {}ms (correlationId={}). " +
          "SessionSubscribeEvent never fired — server-side dispatcher may be blocked or " +
          "the event was lost. Falling back to dispatch anyway.",
        timeoutMs,
        correlationId,
      )
    }
    return waited
  }

  fun cleanup(correlationId: String) {
    val removed = latches.remove(correlationId)
    registeredAt.remove(correlationId)
    logger.debug(
      "WS subscribe latch cleanup (correlationId={}, was-present={}, t={}ms)",
      correlationId,
      removed != null,
      System.currentTimeMillis(),
    )
  }
}
