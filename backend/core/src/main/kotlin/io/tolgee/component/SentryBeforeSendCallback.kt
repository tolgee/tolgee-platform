package io.tolgee.component

import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import org.springframework.stereotype.Component

@Component
class SentryBeforeSendCallback : SentryOptions.BeforeSendCallback {
  companion object {
    val IGNORED_MESSAGE_CONTAINS =
      listOf(
        "Failed to send message to MessageChannel",
        "Cannot render error page for request [/websocket",
      )

    val IGNORED_EXCEPTIONS =
      listOf(
        "FailedDontRequeueException",
        "ClientAbortException",
        "AsyncRequestNotUsableException",
      )
  }

  override fun execute(
    event: SentryEvent,
    hint: Hint,
  ): SentryEvent? {
    if (isMessageIgnored(event)) {
      return null
    }

    if (event.isExceptionIgnored()) {
      return null
    }

    if (event.containsMessage("Invalid connection string") && hasNoException(event)) {
      return null
    }

    return event
  }

  private fun hasNoException(event: SentryEvent) = event.exceptions.isNullOrEmpty()

  private fun SentryEvent.isExceptionIgnored(): Boolean {
    return IGNORED_EXCEPTIONS.any { containsExceptionOfType(it) }
  }

  private fun isMessageIgnored(event: SentryEvent): Boolean {
    return IGNORED_MESSAGE_CONTAINS.any { event.containsMessage(it) }
  }

  private fun SentryEvent.containsMessage(string: String): Boolean {
    return message?.formatted?.contains(string) == true
  }

  private fun SentryEvent.containsExceptionOfType(type: String) =
    exceptions?.any { it.type?.contains(type) == true } == true
}
