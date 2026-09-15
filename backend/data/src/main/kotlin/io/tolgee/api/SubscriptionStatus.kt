package io.tolgee.api

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

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

  /** when we cannot map from stripe status, and when a licence server reports one we do not know */
  @JsonEnumDefaultValue
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
