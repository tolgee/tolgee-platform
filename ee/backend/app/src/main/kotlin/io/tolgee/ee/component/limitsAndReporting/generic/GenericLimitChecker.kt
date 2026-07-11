package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits

open class GenericLimitChecker(
  private val limit: UsageLimits.Limit,
  private val isPayAsYouGo: Boolean,
  /**
   * When plan is fixed (the opposite of pay-as-you-go), this exception will be thrown
   */
  private val includedUsageExceededExceptionProvider: (Long) -> Exception,
  /**
   * When plan is pay-as-you-go, this exception will be thrown
   */
  private val spendingLimitExceededExceptionProvider: (Long) -> Exception,
) {
  fun check(required: Long?) = check { required }

  fun check(requiredProvider: () -> Long?) {
    if (limit.limit < 0) {
      return
    }

    val required = requiredProvider() ?: return

    if (!isPayAsYouGo) {
      if (required > limit.included) {
        throw includedUsageExceededExceptionProvider(required)
      }
      return
    }

    if (required > limit.limit) {
      throw spendingLimitExceededExceptionProvider(required)
    }
  }
}
