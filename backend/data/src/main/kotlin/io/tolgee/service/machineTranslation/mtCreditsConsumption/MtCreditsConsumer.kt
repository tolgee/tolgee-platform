package io.tolgee.service.machineTranslation.mtCreditsConsumption

interface MtCreditsConsumer {
  fun consumeCredits(
    organizationId: Long,
    creditsInCents: Int,
  )

  fun checkPositiveBalance(organizationId: Long)
}
