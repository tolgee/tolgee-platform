package io.tolgee.ee.model

import io.tolgee.ee.data.usageReporting.IUsageToReport
import io.tolgee.model.AuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.util.Date

/**
 * Entity that stores information about usage reporting for the instance.
 * Used to track the number of keys, seats and words that need to be reported to the Tolgee Cloud from a Self-hosted
 * instance.
 */
@Entity
@Table(schema = "ee")
class UsageToReport :
  AuditModel(),
  IUsageToReport {
  /**
   * Fixed ID as we only need a single instance of this entity per installation
   */
  @field:Id
  val id: Int = 1

  /**
   * Number of keys reported in the last report
   */
  override var lastReportedKeys: Long = 0

  /**
   * Number of seats reported in the last report
   */
  override var lastReportedSeats: Long = 0

  /**
   * Number of words reported in the last report
   */
  @ColumnDefault("0")
  override var lastReportedWords: Long = 0

  /**
   * Number of keys that need to be reported in the next report
   */
  override var keysToReport: Long = 0

  /**
   * Number of seats that need to be reported in the next report
   */
  override var seatsToReport: Long = 0

  /**
   * Number of words that need to be reported in the next report
   */
  @ColumnDefault("0")
  override var wordsToReport: Long = 0

  /**
   * Raised by write paths; the periodic report takes the count, because counting words on the
   * instance is a full aggregation.
   */
  @ColumnDefault("false")
  override var wordsDirty: Boolean = false

  /**
   * When the instance word count was last taken. Its own clock, because the shared [reportedAt] is
   * advanced by every key and seat report, so on an instance with steady key activity it would
   * never reopen — and it throttles the count rather than the send, because the count is the
   * expensive half.
   */
  @ColumnDefault("'1970-01-01 00:00:00'")
  @Column(nullable = false)
  override var wordsCountedAt: Date = Date(0)

  /**
   * Timestamp of when the last report was made
   */
  override lateinit var reportedAt: Date
}
