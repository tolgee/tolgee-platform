package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.generic.WordsLimitChecker
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WordsLimitCheckerTest {
  private fun unlimitedLimit() = UsageLimits.Limit(included = -1, limit = -1)

  private fun usageLimits(words: UsageLimits.Limit = unlimitedLimit()) =
    UsageLimits(
      isPayAsYouGo = false,
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
}
