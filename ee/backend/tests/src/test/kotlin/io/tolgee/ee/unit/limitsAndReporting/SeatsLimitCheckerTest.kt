package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.generic.SeatsLimitChecker
import io.tolgee.exceptions.limits.PlanLimitExceededSeatsException
import io.tolgee.exceptions.limits.PlanLimitExceededStringsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededSeatsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeatsLimitCheckerTest {
  private fun unlimitedLimit() = UsageLimits.Limit(included = -1, limit = -1)

  private fun usageLimits(
    isPayAsYouGo: Boolean = false,
    seats: UsageLimits.Limit = unlimitedLimit(),
  ) = UsageLimits(
    isPayAsYouGo = isPayAsYouGo,
    isTrial = false,
    strings = unlimitedLimit(),
    keys = unlimitedLimit(),
    seats = seats,
    mtCreditsInCents = unlimitedLimit(),
  )

  @Test
  fun `default fixed plan over seats limit - throws PlanLimitExceededSeatsException`() {
    val limits = usageLimits(seats = UsageLimits.Limit(included = 5, limit = 5))
    val checker = SeatsLimitChecker(limits)

    assertThrows<PlanLimitExceededSeatsException> { checker.check { 6L } }
  }

  @Test
  fun `default payg over seats limit - throws PlanSpendingLimitExceededSeatsException`() {
    val limits = usageLimits(isPayAsYouGo = true, seats = UsageLimits.Limit(included = 5, limit = 10))
    val checker = SeatsLimitChecker(limits)

    assertThrows<PlanSpendingLimitExceededSeatsException> { checker.check { 11L } }
  }

  @Test
  fun `custom includedUsageExceededExceptionProvider - custom exception thrown`() {
    val limits = usageLimits(seats = UsageLimits.Limit(included = 5, limit = 5))
    val checker =
      SeatsLimitChecker(
        limits = limits,
        includedUsageExceededExceptionProvider = { req -> PlanLimitExceededStringsException(req, 5L) },
      )

    assertThrows<PlanLimitExceededStringsException> { checker.check { 6L } }
  }
}
