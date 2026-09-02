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
  val mtCreditsInCents: Limit,
  val words: Limit,
  /**
   * Whether the plan bills on hosted words at all. Not derivable from [words]: the licence server
   * sends `Limit(-1, -1)` both for a keys-and-seats plan (words do not apply) and for a word plan
   * whose allowance is unlimited or negotiated, so the numbers cannot tell the two apart.
   */
  val metersWords: Boolean = false,
) {
  data class Limit(
    /**
     * What's included in the plan
     *
     * -1 if unlimited
     */
    val included: Long,
    /**
     *  What's the maximum value before using all the usage from spending limit
     *
     * -1 if unlimited
     */
    val limit: Long,
    /**
     * Null where the licence does not model auto-upgrade for this metric, which today is every
     * metric but words.
     */
    val autoUpgradeEffective: Boolean? = null,
  ) {
    /**
     * A metered plan whose allowance is unlimited (-1) or negotiated (-2) sends no ceiling, and
     * blocking on one would refuse every write — so enforcement is narrower than metering.
     */
    val isEnforced: Boolean get() = limit >= 0
  }
}
