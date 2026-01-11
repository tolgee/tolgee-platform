package io.tolgee.service.machineTranslation.mtCreditsConsumption

import io.tolgee.dtos.MtCreditBalanceDto

/**
 * This interface can be used from public code to handle machine translation credits consumption as well as
 * balances retrieval.
 *
 * For Billing, we use usage events to handle the credits consumption.
 *
 * In the other hand, on self-hosted instances, we use the legacy MT Credit buckets, so the users are able to limit
 * the usage of the machine translation service.
 */
interface MtCreditsService {
  fun consumeCredits(
    organizationId: Long,
    creditsInCents: Int,
  )

  fun checkPositiveBalance(organizationId: Long)

  fun getCreditBalances(organizationId: Long): MtCreditBalanceDto
}
