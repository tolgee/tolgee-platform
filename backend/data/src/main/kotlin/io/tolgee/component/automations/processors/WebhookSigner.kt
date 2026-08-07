package io.tolgee.component.automations.processors

import io.tolgee.component.CurrentDateProvider
import io.tolgee.fixtures.computeHmacSha256
import org.springframework.stereotype.Component

/**
 * The one signature scheme every outbound signed POST Tolgee makes uses — project webhooks and app
 * lifecycle deliveries alike. A second scheme would mean an app author verifying two different
 * things depending on which of our features sent the request.
 */
@Component
class WebhookSigner(
  private val currentDateProvider: CurrentDateProvider,
) {
  fun signatureHeader(
    payload: String,
    key: String,
  ): String {
    val timestamp = currentDateProvider.date.time
    val signature = computeHmacSha256(key, "$timestamp.$payload")
    return """{"timestamp": $timestamp, "signature": "$signature"}"""
  }

  companion object {
    const val SIGNATURE_HEADER = "Tolgee-Signature"
  }
}
