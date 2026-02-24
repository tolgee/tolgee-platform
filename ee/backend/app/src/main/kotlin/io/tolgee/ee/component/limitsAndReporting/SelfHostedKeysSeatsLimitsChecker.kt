package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.ee.component.limitsAndReporting.generic.KeysLimitChecker
import io.tolgee.ee.component.limitsAndReporting.generic.SeatsLimitChecker
import org.springframework.context.ApplicationContext

/**
 * Checks whether self-hosted instance doesn't exceed the limits.
 */
class SelfHostedKeysSeatsLimitsChecker(
  private val keys: Long?,
  private val seats: Long?,
  private val applicationContext: ApplicationContext,
) {
  fun check() {
    checkKeysLimits()
    checkSeatLimits()
  }

  private fun checkKeysLimits() {
    KeysLimitChecker(limits = limits).check(keys)
  }

  private fun checkSeatLimits() {
    SeatsLimitChecker(limits = limits).check(seats)
  }

  private val limits by lazy {
    selfHostedLimitsProvider.getLimits()
  }

  private val selfHostedLimitsProvider by lazy { applicationContext.getBean(SelfHostedLimitsProvider::class.java) }
}
