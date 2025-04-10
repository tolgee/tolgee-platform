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
    keys ?: return
    KeysLimitChecker(required = keys, limits = limits).check()
  }

  private fun checkSeatLimits() {
    seats ?: return
    SeatsLimitChecker(required = seats, limits = limits).check()
  }

  private val limits by lazy {
    selfHostedLimitsProvider.getLimits()
  }

  private val selfHostedLimitsProvider by lazy { applicationContext.getBean(SelfHostedLimitsProvider::class.java) }
}
