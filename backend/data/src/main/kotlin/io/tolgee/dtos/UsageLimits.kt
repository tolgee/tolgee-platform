package io.tolgee.dtos

/**
 * This class represents usage limits for a subscription plan
 *
 * Translation slots are legacy unit
 */
data class UsageLimits(
  val isPayAsYouGo: Boolean,
  val isTrial: Boolean,
  val strings: Limit,
  val keys: Limit,
  val seats: Limit,
  //
  // Legacy units
  //
  val translationSlots: Limit,
) {
  data class Limit(
    /** What's included in the plan */
    val included: Long,
    /** What's the maximum value before using all the usage from spending limit */
    val limit: Long,
  )
}
