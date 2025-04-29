package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits

class GenericLimitChecker(
  private val required: Long,
  private val limit: UsageLimits.Limit,
  private val isPayAsYouGo: Boolean,
  /**
   * When plan is fixed (the opposite of pay-as-you-go), this exception will be thrown
   */
  private val includedUsageExceededExceptionProvider: () -> Exception,
  /**
   * When plan is pay-as-you-go, this exception will be thrown
   */
  private val spendingLimitExceededExceptionProvider: () -> Exception,
) {
  fun checkLimit() {
    if (limit.limit < 0) {
      return
    }

    if (!isPayAsYouGo) {
      if (required > limit.included) {
        throw includedUsageExceededExceptionProvider()
      }
      return
    }

    if (required > limit.limit) {
      throw spendingLimitExceededExceptionProvider()
    }
  }
}
