package io.tolgee.ee.model

import io.tolgee.ee.data.usageReporting.IUsageToReport
import io.tolgee.model.AuditModel
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Date

/**
 * Entity that stores information about usage reporting for the instance.
 * Used to track the number of keys and seats that need to be reported to the Tolgee Cloud from a Self-hosted instance.
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
   * Number of keys that need to be reported in the next report
   */
  override var keysToReport: Long = 0

  /**
   * Number of seats that need to be reported in the next report
   */
  override var seatsToReport: Long = 0

  /**
   * Timestamp of when the last report was made
   */
  override lateinit var reportedAt: Date
}
