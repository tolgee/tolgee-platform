package io.tolgee.ee.api.v2.hateoas

import org.springframework.hateoas.RepresentationModel
import java.math.BigDecimal

@Suppress("unused")
class PrepareSetEeLicenceKeyModel(
  val pricePerSeatMonthly: BigDecimal = BigDecimal.ZERO,
  val daysUntilPeriodEnd: BigDecimal = BigDecimal.ZERO,
  val pricePerSeatDaily: BigDecimal = BigDecimal.ZERO,
  val estimatedTotalThisPeriod: BigDecimal = BigDecimal.ZERO,
  val seatCount: Long = 0,
) : RepresentationModel<PrepareSetEeLicenceKeyModel>()
