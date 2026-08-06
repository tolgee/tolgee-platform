package io.tolgee.service.apps.lifecycle

import java.util.Date

/**
 * A delivery waiting to be sent or retried. Lives only in the sending process — see [io.tolgee.model.apps.AppDelivery]
 * for why the payload is never persisted.
 */
class PendingAppDelivery(
  val deliveryId: Long,
  val targetUrl: String,
  val payload: String,
  val signingSecret: String,
) {
  @Volatile
  var attempts: Int = 0

  @Volatile
  var nextAttemptAt: Date? = null
}
