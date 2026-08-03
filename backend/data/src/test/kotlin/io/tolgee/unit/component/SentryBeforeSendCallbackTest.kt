package io.tolgee.unit.component

import com.azure.storage.blob.BlobServiceClientBuilder
import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import io.tolgee.component.SentryBeforeSendCallback
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SentryBeforeSendCallbackTest {
  private val callback = SentryBeforeSendCallback()

  @Test
  fun `drops the websocket noise`() {
    assertThat(callback.execute(eventWithMessage("Failed to send message to MessageChannel x"), Hint())).isNull()
    assertThat(
      callback.execute(eventWithMessage("Cannot render error page for request [/websocket/x]"), Hint()),
    ).isNull()
  }

  @Test
  fun `drops the exception types we never want reported`() {
    listOf(
      "FailedDontRequeueException",
      "ClientAbortException",
      "AsyncRequestNotUsableException",
      "RequestRejectedException",
      "MissingPathVariableException",
    ).forEach {
      assertThat(callback.execute(eventOfType(it), Hint())).describedAs("event of type %s", it).isNull()
    }
  }

  /**
   * Drives the Azure SDK for the wording rather than repeating it, so a reword fails here instead of
   * silently letting these events through. AzureFileStorageFactory no longer depends on this string,
   * but this filter still does.
   */
  @Test
  fun `drops an invalid azure connection string reported without an exception`() {
    val fromSdk =
      assertThatThrownBy { BlobServiceClientBuilder().connectionString("not a connection string") }
        .isInstanceOf(IllegalArgumentException::class.java)
        .actual()
        .message!!

    assertThat(callback.execute(eventWithMessage(fromSdk), Hint())).isNull()
  }

  @Test
  fun `keeps an invalid azure connection string that came with an exception`() {
    val event = eventWithMessage("Invalid connection string.")
    event.exceptions = listOf(SentryException().apply { type = "IllegalArgumentException" })

    assertThat(callback.execute(event, Hint())).isSameAs(event)
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

  private fun eventOfType(type: String) =
    SentryEvent().apply {
      exceptions = listOf(SentryException().apply { this.type = type })
    }
}
