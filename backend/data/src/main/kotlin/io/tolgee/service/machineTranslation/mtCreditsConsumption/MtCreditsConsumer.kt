package io.tolgee.service.machineTranslation.mtCreditsConsumption

interface MtCreditsConsumer {
  fun consumeCredits(
    organizationId: Long,
    amount: Int,
  )

  fun checkPositiveBalance(organizationId: Long)
}
