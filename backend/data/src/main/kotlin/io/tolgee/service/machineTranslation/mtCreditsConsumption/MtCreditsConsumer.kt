package io.tolgee.service.machineTranslation.mtCreditsConsumption

import io.tolgee.dtos.MtCreditBalanceDto

interface MtCreditsConsumer {
  fun consumeCredits(
    organizationId: Long,
    creditsInCents: Int,
  )

  fun checkPositiveBalance(organizationId: Long)

  fun getCreditBalances(organizationId: Long): MtCreditBalanceDto
}
