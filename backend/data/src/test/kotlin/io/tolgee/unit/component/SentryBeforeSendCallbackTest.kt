package io.tolgee.unit.component

import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import io.tolgee.component.SentryBeforeSendCallback
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Every literal here is owned by something outside this repository — Spring's websocket logging, the
 * Azure SDK, and the simple names of framework exception classes — so nothing fails at compile time
 * when one of them changes and the filter silently stops dropping the event.
 */
class SentryBeforeSendCallbackTest {
  private val callback = SentryBeforeSendCallback()

  @Test
  fun `drops the messages we never want reported`() {
    SentryBeforeSendCallback.IGNORED_MESSAGE_CONTAINS.forEach {
      assertThat(callback.execute(eventWithMessage(it), Hint()))
        .describedAs("event carrying %s", it)
        .isNull()
    }
  }

  @Test
  fun `drops an invalid azure connection string reported without an exception`() {
    assertThat(callback.execute(eventWithMessage("Invalid connection string."), Hint())).isNull()
  }

  @Test
  fun `keeps an invalid azure connection string that came with an exception`() {
    val event = eventWithMessage("Invalid connection string.")
    event.exceptions = listOf(SentryException().apply { type = "IllegalArgumentException" })

    assertThat(callback.execute(event, Hint())).isSameAs(event)
  }

  @Test
  fun `drops the exception types we never want reported`() {
    SentryBeforeSendCallback.IGNORED_EXCEPTIONS.forEach {
      val event = SentryEvent()
      event.exceptions = listOf(SentryException().apply { type = it })

      assertThat(callback.execute(event, Hint())).describedAs("event of type %s", it).isNull()
    }
  }

  @Test
  fun `keeps anything else`() {
    val event = eventWithMessage("Something actually broke")
    event.exceptions = listOf(SentryException().apply { type = "IllegalStateException" })

    assertThat(callback.execute(event, Hint())).isSameAs(event)
  }

  private fun eventWithMessage(formatted: String) =
    SentryEvent().apply {
      message = Message().apply { this.formatted = formatted }
    }
}
