package io.tolgee.dtos

import java.util.*

data class MtCreditBalanceDto(
  val creditBalance: Long,
  val bucketSize: Long,
  val refilledAt: Date,
  val nextRefillAt: Date,
)
