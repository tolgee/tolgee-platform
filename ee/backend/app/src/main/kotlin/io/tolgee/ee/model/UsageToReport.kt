package io.tolgee.ee.model

import io.tolgee.ee.data.usageReporting.IUsageToReport
import io.tolgee.model.AuditModel
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(schema = "ee")
class UsageToReport : AuditModel(), IUsageToReport {
  @field:Id
  val id: Int = 1

  override var lastReportedKeys: Long = 0

  override var lastReportedSeats: Long = 0

  override var keysToReport: Long = 0

  override var seatsToReport: Long = 0
  override lateinit var reportedAt: Date
}
