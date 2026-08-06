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

    assertThat(limits).isEqualTo(UsageLimits.Limit(included = 100000, limit = 100000))
  }

  @Test
  fun `no word limit on the subscription - returns unlimited (behaviour preserving)`() {
    val limits =
      provider(subscriptionDto(includedWords = -1, wordsLimit = -1))
        .getLimits()
        .words

    assertThat(limits).isEqualTo(UsageLimits.Limit(included = -1, limit = -1))
  }
}
