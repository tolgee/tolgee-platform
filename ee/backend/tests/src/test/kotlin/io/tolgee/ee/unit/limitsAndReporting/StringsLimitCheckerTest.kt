package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.generic.StringsLimitChecker
import io.tolgee.exceptions.limits.PlanLimitExceededStringsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededStringsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StringsLimitCheckerTest {
  private fun unlimitedLimit() = UsageLimits.Limit(included = -1, limit = -1)

  private fun usageLimits(
    isPayAsYouGo: Boolean = false,
    strings: UsageLimits.Limit = unlimitedLimit(),
  ) = UsageLimits(
    isPayAsYouGo = isPayAsYouGo,
    isTrial = false,
    strings = strings,
    keys = unlimitedLimit(),
    seats = unlimitedLimit(),
    mtCreditsInCents = unlimitedLimit(),
  )

  @Test
  fun `zero strings limit - throws PlanLimitExceededStringsException (zero is a real limit)`() {
    val checker = StringsLimitChecker(usageLimits(strings = UsageLimits.Limit(included = 0, limit = 0)))

    assertThrows<PlanLimitExceededStringsException> { checker.check { 999L } }
  }

  @Test
  fun `fixed plan over strings limit - throws PlanLimitExceededStringsException with correct values`() {
    val limits = usageLimits(strings = UsageLimits.Limit(included = 100, limit = 100))
    val checker = StringsLimitChecker(limits)

    val ex = assertThrows<PlanLimitExceededStringsException> { checker.check { 150L } }

    assertThat(ex.params).containsExactly(150L, 100L)
  }

  @Test
  fun `payg over strings limit - throws PlanSpendingLimitExceededStringsException with correct values`() {
    val limits = usageLimits(isPayAsYouGo = true, strings = UsageLimits.Limit(included = 100, limit = 200))
    val checker = StringsLimitChecker(limits)

    val ex = assertThrows<PlanSpendingLimitExceededStringsException> { checker.check { 250L } }

    assertThat(ex.params).containsExactly(250L, 200L)
  }
}
