package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.generic.WordsLimitChecker
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededWordsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WordsLimitCheckerTest {
  private fun unlimitedLimit() = UsageLimits.Limit(included = -1, limit = -1)

  private fun usageLimits(
    isPayAsYouGo: Boolean = false,
    words: UsageLimits.Limit = unlimitedLimit(),
  ) = UsageLimits(
    isPayAsYouGo = isPayAsYouGo,
    isTrial = false,
    strings = unlimitedLimit(),
    keys = unlimitedLimit(),
    seats = unlimitedLimit(),
    mtCreditsInCents = unlimitedLimit(),
    words = words,
  )

  @Test
  fun `over words limit - throws PlanLimitExceededWordsException`() {
    val n = 100L
    val limits = usageLimits(words = UsageLimits.Limit(included = n, limit = n))
    val checker = WordsLimitChecker(limits)

    val ex = assertThrows<PlanLimitExceededWordsException> { checker.check { n + 1 } }

    assertThat(ex.params).containsExactly(n + 1, n)
  }

  @Test
  fun `at words limit - does not throw`() {
    val n = 100L
    val limits = usageLimits(words = UsageLimits.Limit(included = n, limit = n))
    val checker = WordsLimitChecker(limits)

    checker.check { n }
  }

  @Test
  fun `unlimited words limit - does not throw`() {
    val checker = WordsLimitChecker(usageLimits())

    checker.check { 999_999L }
  }

  @Test
  fun `zero words limit - throws PlanLimitExceededWordsException (zero is a real limit)`() {
    val checker = WordsLimitChecker(usageLimits(words = UsageLimits.Limit(included = 0, limit = 0)))

    assertThrows<PlanLimitExceededWordsException> { checker.check { 1L } }
  }

  @Test
  fun `payg over words limit - throws PlanSpendingLimitExceededWordsException with correct values`() {
    val limits = usageLimits(isPayAsYouGo = true, words = UsageLimits.Limit(included = 100, limit = 200))
    val checker = WordsLimitChecker(limits)

    val ex = assertThrows<PlanSpendingLimitExceededWordsException> { checker.check { 250L } }

    assertThat(ex.params).containsExactly(250L, 200L)
  }

  @Test
  fun `payg under words spending limit but over included - does not throw`() {
    val limits = usageLimits(isPayAsYouGo = true, words = UsageLimits.Limit(included = 100, limit = 200))

    WordsLimitChecker(limits).check { 150L }
  }
}
