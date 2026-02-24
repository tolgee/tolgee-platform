package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.generic.KeysLimitChecker
import io.tolgee.exceptions.limits.PlanLimitExceededKeysException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededKeysException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KeysLimitCheckerTest {
  private fun unlimitedLimit() = UsageLimits.Limit(included = -1, limit = -1)

  private fun usageLimits(
    isPayAsYouGo: Boolean = false,
    keys: UsageLimits.Limit = unlimitedLimit(),
  ) = UsageLimits(
    isPayAsYouGo = isPayAsYouGo,
    isTrial = false,
    strings = unlimitedLimit(),
    keys = keys,
    seats = unlimitedLimit(),
    mtCreditsInCents = unlimitedLimit(),
  )

  @Test
  fun `zero keys limit - throws PlanLimitExceededKeysException (zero is a real limit)`() {
    val checker = KeysLimitChecker(usageLimits(keys = UsageLimits.Limit(included = 0, limit = 0)))

    assertThrows<PlanLimitExceededKeysException> { checker.check { 999L } }
  }

  @Test
  fun `fixed plan over keys limit - throws PlanLimitExceededKeysException with correct values`() {
    val limits = usageLimits(keys = UsageLimits.Limit(included = 100, limit = 100))
    val checker = KeysLimitChecker(limits)

    val ex = assertThrows<PlanLimitExceededKeysException> { checker.check { 150L } }

    assertThat(ex.params).containsExactly(150L, 100L)
  }

  @Test
  fun `payg over keys limit - throws PlanSpendingLimitExceededKeysException with correct values`() {
    val limits = usageLimits(isPayAsYouGo = true, keys = UsageLimits.Limit(included = 100, limit = 200))
    val checker = KeysLimitChecker(limits)

    val ex = assertThrows<PlanSpendingLimitExceededKeysException> { checker.check { 250L } }

    assertThat(ex.params).containsExactly(250L, 200L)
  }
}
