package io.tolgee.api

enum class SubscriptionStatus(
  val active: Boolean,
) {
  ACTIVE(true),
  CANCELED(false),
  PAST_DUE(false),
  UNPAID(false),
  ERROR(false),
  TRIALING(true),

  // might be stored on the EE side, but not license server (billing) side
  KEY_USED_BY_ANOTHER_INSTANCE(false),
}
