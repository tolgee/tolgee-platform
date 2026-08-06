package io.tolgee.ee.data.usageReporting

import java.util.Date

interface IUsageToReport {
  val lastReportedKeys: Long

  val lastReportedSeats: Long

  val lastReportedWords: Long

  val keysToReport: Long

  val seatsToReport: Long

  val wordsToReport: Long

  val reportedAt: Date
}
