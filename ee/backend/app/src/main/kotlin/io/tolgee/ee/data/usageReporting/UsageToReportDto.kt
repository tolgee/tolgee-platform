package io.tolgee.ee.data.usageReporting

import java.util.Date

data class UsageToReportDto(
  override val lastReportedKeys: Long,
  override val lastReportedSeats: Long,
  override val keysToReport: Long,
  override val seatsToReport: Long,
  override val reportedAt: Date,
) : IUsageToReport
