package io.tolgee.publicBilling

enum class CloudSubscriptionPlanType(
  val payAsYouGo: Boolean,
  val usesSlots: Boolean,
) {
  /**
   * Non-free plans, where user cay be charged by their usage
   */
  PAY_AS_YOU_GO(true, false),

  /**
   * Plans where user cannot exceed included translations
   */
  FIXED(false, false),
}
