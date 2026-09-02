package io.tolgee.ee.unit.limitsAndReporting

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.component.limitsAndReporting.SelfHostedLimitsProvider
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SelfHostedLimitsProviderTest {
  private fun subscriptionDto(
    includedWords: Long,
    wordsLimit: Long,
    metersWords: Boolean = true,
  ) = EeSubscriptionDto(
    licenseKey = "mock",
    name = "Plan",
    enabledFeatures = arrayOf(),
    includedKeys = -1,
    includedSeats = -1,
    isPayAsYouGo = false,
    keysLimit = -1,
    seatsLimit = -1,
    includedWords = includedWords,
    wordsLimit = wordsLimit,
    metersWords = metersWords,
  )

  private fun provider(dto: EeSubscriptionDto): SelfHostedLimitsProvider {
    val eeSubscriptionServiceImpl = mock<EeSubscriptionServiceImpl>()
    whenever(eeSubscriptionServiceImpl.findSubscriptionDto()).thenReturn(dto)
    return SelfHostedLimitsProvider(eeSubscriptionServiceImpl)
  }

  @Test
  fun `returns the words limit stored on the subscription`() {
    val limits =
      provider(subscriptionDto(includedWords = 100000, wordsLimit = 100000))
        .getLimits()
        .words

    assertThat(limits).isEqualTo(
      UsageLimits.Limit(included = 100000, limit = 100000, autoUpgradeEffective = false),
    )
  }

  @Test
  fun `no word limit on the subscription - returns unlimited (behaviour preserving)`() {
    val limits =
      provider(subscriptionDto(includedWords = -1, wordsLimit = -1))
        .getLimits()
        .words

    assertThat(limits).isEqualTo(
      UsageLimits.Limit(included = -1, limit = -1, autoUpgradeEffective = false),
    )
  }

  /**
   * The numbers cannot answer this: a keys-and-seats licence and a word plan with an unlimited or
   * negotiated allowance both arrive as Limit(-1, -1) / Limit(-2, -2).
   */
  @Test
  fun `meters words on a word plan whose allowance carries no number`() {
    listOf(-1L, -2L).forEach { allowance ->
      assertThat(
        provider(subscriptionDto(includedWords = allowance, wordsLimit = allowance))
          .getLimits()
          .metersWords,
      ).withFailMessage("allowance %d should still be metered", allowance).isTrue()
    }
  }

  @Test
  fun `does not meter words on a keys-and-seats licence, whatever the word numbers say`() {
    assertThat(
      provider(subscriptionDto(includedWords = 100000, wordsLimit = 100000, metersWords = false))
        .getLimits()
        .metersWords,
    ).isFalse()
  }

  @Test
  fun `meters nothing without a subscription`() {
    val service = mock<EeSubscriptionServiceImpl>()
    whenever(service.findSubscriptionDto()).thenReturn(null)

    assertThat(SelfHostedLimitsProvider(service).getLimits().metersWords).isFalse()
  }
}
