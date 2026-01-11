package io.tolgee.api

enum class SubscriptionStatus(
  val active: Boolean,
) {
  ACTIVE(true),
  CANCELED(false),
  PAST_DUE(true),
  UNPAID(false),
  ERROR(false),
  TRIALING(true),

  /** might be stored on the EE side, but not license server (billing) side */
  KEY_USED_BY_ANOTHER_INSTANCE(false),

  /** when we cannot map from stripe status */
  UNKNOWN(false),
  ;

  companion object {
    fun fromStripeStatus(stripeStatus: String?): SubscriptionStatus {
      return when (stripeStatus) {
        "active" -> ACTIVE
        "canceled" -> CANCELED
        "past_due" -> PAST_DUE
        "unpaid" -> UNPAID
        "trialing" -> TRIALING
        else -> UNKNOWN
      }
    }
  }
}
