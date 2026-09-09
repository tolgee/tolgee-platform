package io.tolgee.dtos.apps

/**
 * What happened when Tolgee tried to hand a secret-carrying event to an app, synchronously, so the
 * dialog that triggered it can tell the operator whether the app got the credentials or whether they
 * still have to copy them by hand.
 *
 * A failure is a value here, never an exception: the credentials were already disclosed in the
 * response, so a dead app host must not roll back the registration or rotation that produced them.
 */
data class AppLifecycleDeliveryOutcome(
  /** False when there was nothing to deliver to (no app, no base URL). */
  val attempted: Boolean,
  val delivered: Boolean,
  /** Short, safe-to-show reason when [attempted] but not [delivered]; null otherwise. */
  val error: String?,
) {
  companion object {
    val NOT_ATTEMPTED = AppLifecycleDeliveryOutcome(attempted = false, delivered = false, error = null)
    val DELIVERED = AppLifecycleDeliveryOutcome(attempted = true, delivered = true, error = null)

    fun failed(reason: String): AppLifecycleDeliveryOutcome =
      AppLifecycleDeliveryOutcome(attempted = true, delivered = false, error = reason.take(MAX_ERROR_LENGTH))

    private const val MAX_ERROR_LENGTH = 300
  }
}
