package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.generic.GenericLimitChecker
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GenericLimitCheckerTest {
  private fun fixedLimit(
    included: Long,
    total: Long = included,
  ) = UsageLimits.Limit(included = included, limit = total)

  private fun unlimitedLimit() = UsageLimits.Limit(included = -1, limit = -1)

  private fun zeroLimit() = UsageLimits.Limit(included = 0, limit = 0)

  private fun checker(
    limit: UsageLimits.Limit,
    isPayAsYouGo: Boolean = false,
    includedProvider: (Long) -> Exception = { req -> RuntimeException("included: $req") },
    spendingProvider: (Long) -> Exception = { req -> RuntimeException("spending: $req") },
  ) = GenericLimitChecker(
    limit = limit,
    isPayAsYouGo = isPayAsYouGo,
    includedUsageExceededExceptionProvider = includedProvider,
    spendingLimitExceededExceptionProvider = spendingProvider,
  )

  @Test
  fun `zero limit - throws included exception (not treated as unlimited)`() {
    val checker = checker(zeroLimit())

    val ex = assertThrows<RuntimeException> { checker.check { 999L } }

    assert(ex.message == "included: 999")
  }

  @Test
  fun `minus one limit - no exception and provider not called`() {
    var providerCalled = false
    val checker =
      checker(
        unlimitedLimit(),
        includedProvider = {
          providerCalled = true
          RuntimeException()
        },
      )

    assertDoesNotThrow { checker.check { 999L } }
    assertFalse(providerCalled)
  }

  @Test
  fun `provider returns null - no exception`() {
    val checker = checker(fixedLimit(100))
    assertDoesNotThrow { checker.check { null } }
  }

  @Test
  fun `fixed plan - required within included - no exception`() {
    val checker = checker(fixedLimit(100))
    assertDoesNotThrow { checker.check { 100L } }
  }

  @Test
  fun `fixed plan - required exceeds included - throws included exception`() {
    val checker = checker(fixedLimit(included = 100, total = 200))

    val ex = assertThrows<RuntimeException> { checker.check { 150L } }

    assert(ex.message == "included: 150")
  }

  @Test
  fun `payg - required exceeds included but within limit - no exception`() {
    val checker = checker(fixedLimit(included = 100, total = 200), isPayAsYouGo = true)
    assertDoesNotThrow { checker.check { 150L } }
  }

  @Test
  fun `payg - required equals limit - no exception`() {
    val checker = checker(fixedLimit(included = 100, total = 200), isPayAsYouGo = true)
    assertDoesNotThrow { checker.check { 200L } }
  }

  @Test
  fun `payg - required exceeds limit - throws spending exception`() {
    val checker = checker(fixedLimit(included = 100, total = 200), isPayAsYouGo = true)

    val ex = assertThrows<RuntimeException> { checker.check { 201L } }

    assert(ex.message == "spending: 201")
  }

  @Test
  fun `check nullable overload with value - behaves same as lambda overload`() {
    val checker = checker(fixedLimit(included = 100, total = 200))

    val ex = assertThrows<RuntimeException> { checker.check(150L) }

    assert(ex.message == "included: 150")
  }

  @Test
  fun `check nullable overload with null - no exception`() {
    val checker = checker(fixedLimit(100))
    assertDoesNotThrow { checker.check(null) }
  }
}
