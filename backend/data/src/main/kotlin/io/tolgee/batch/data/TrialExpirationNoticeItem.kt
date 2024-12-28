package io.tolgee.batch.data

import java.util.*

data class TrialExpirationNoticeItem(
  val trialEnd: Date,
  val organizationId: Long,
  val daysBefore: Int,
)
