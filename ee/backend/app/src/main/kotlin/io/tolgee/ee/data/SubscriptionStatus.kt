package io.tolgee.ee.data

enum class SubscriptionStatus {
  ACTIVE,
  CANCELED,
  PAST_DUE,
  UNPAID,
  ERROR,

  // might be stored on the EE side, but not license server (billing) side
  KEY_USED_BY_ANOTHER_INSTANCE,
}
