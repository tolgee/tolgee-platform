package io.tolgee.dtos

import java.util.Date

data class MtCreditBalanceDto(
  /** Used credits in cents */
  val usedCredits: Long,
  /** Remaining credits in cents */
  val creditBalance: Long,
  /** Credits included in the plan (or in the Bucket size) in cents */
  val bucketSize: Long,
  val refilledAt: Date,
  val nextRefillAt: Date,
)
