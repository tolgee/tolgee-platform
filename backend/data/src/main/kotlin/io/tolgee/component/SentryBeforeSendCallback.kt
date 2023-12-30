package io.tolgee.component

import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import org.springframework.stereotype.Component

@Component
class SentryBeforeSendCallback : SentryOptions.BeforeSendCallback {
  override fun execute(
    event: SentryEvent,
    hint: Hint,
  ): SentryEvent? {
    if (event.containsMessage("Failed to send message to MessageChannel")) {
      return null
    }

    if (event.containsExceptionOfType("FailedDontRequeueException")) {
      return null
    }

    if (event.containsExceptionOfType("ClientAbortException")) {
      return null
    }

    if (event.containsMessage("Cannot render error page for request [/websocket")) return null

    return event
  }

  private fun SentryEvent.containsMessage(string: String): Boolean {
    return message?.formatted?.contains(string) == true
  }

  private fun SentryEvent.containsExceptionOfType(type: String) =
    exceptions?.any { it.type?.contains(type) == true } == true
}
