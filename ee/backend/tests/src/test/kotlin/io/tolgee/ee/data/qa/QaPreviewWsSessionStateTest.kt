package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

class QaPreviewWsSessionStateTest {
  private lateinit var state: QaPreviewWsSessionState

  @BeforeEach
  fun setup() {
    state =
      QaPreviewWsSessionState(
        projectId = 1L,
        baseText = "Hello",
        baseLanguageTag = "en",
        languageTag = "cs",
        keyId = 1L,
        translationId = 1L,
        enabledCheckTypes = listOf(QaCheckType.EMPTY_TRANSLATION),
        isPlural = false,
        baseVariants = null,
        maxCharLimit = null,
        icuPlaceholders = true,
        organizationOwnerId = 1L,
        glossaryEnabled = false,
      )
  }

  @Test
  fun `tryAcceptMessage accepts messages within rate limit`() {
    assertThat(state.tryAcceptMessage()).isTrue()

    repeat(QaPreviewWsSessionState.MAX_MESSAGES_PER_WINDOW - 1) {
      assertThat(state.tryAcceptMessage()).isTrue()
    }
  }

  @Test
  fun `tryAcceptMessage rejects messages exceeding rate limit`() {
    repeat(QaPreviewWsSessionState.MAX_MESSAGES_PER_WINDOW.toInt()) {
      state.tryAcceptMessage()
    }

    assertThat(state.tryAcceptMessage()).isFalse()
  }

  @Test
  fun `tryAcceptMessage resets after time window expires`() {
    repeat(QaPreviewWsSessionState.MAX_MESSAGES_PER_WINDOW.toInt()) {
      state.tryAcceptMessage()
    }
    assertThat(state.tryAcceptMessage()).isFalse()

    // Simulate time window expiry
    state.lastMessageTime =
      java.time.Instant.now().minusMillis(
        QaPreviewWsSessionState.RATE_LIMIT_WINDOW_MS + 100,
      )

    assertThat(state.tryAcceptMessage()).isTrue()
  }

  @Test
  fun `tryAcceptMessage is thread-safe under concurrent access`() {
    val threadCount = 50
    val barrier = CyclicBarrier(threadCount)
    val latch = CountDownLatch(threadCount)
    val acceptedCount = AtomicInteger(0)
    val rejectedCount = AtomicInteger(0)

    val threads =
      (1..threadCount).map {
        Thread {
          barrier.await()
          if (state.tryAcceptMessage()) {
            acceptedCount.incrementAndGet()
          } else {
            rejectedCount.incrementAndGet()
          }
          latch.countDown()
        }
      }

    threads.forEach { it.start() }
    latch.await()

    assertThat(acceptedCount.get() + rejectedCount.get()).isEqualTo(threadCount)
    assertThat(acceptedCount.get()).isLessThanOrEqualTo(
      QaPreviewWsSessionState.MAX_MESSAGES_PER_WINDOW.toInt(),
    )
    assertThat(rejectedCount.get()).isGreaterThan(0)
  }
}
