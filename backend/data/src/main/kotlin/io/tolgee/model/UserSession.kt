package io.tolgee.model

import io.tolgee.model.enums.UserSessionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * One row per device lineage: a token refresh keeps the device id and updates this row, so revoking
 * it kills every token ever minted for that device.
 */
@Entity
@Table(
  name = "user_session",
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["device_id"],
      name = "user_session_device_id",
    ),
  ],
  indexes = [
    Index(columnList = "user_account_id"),
    Index(columnList = "expires_at"),
  ],
)
class UserSession : StandardAuditModel() {
  @Column(name = "device_id", nullable = false, length = 36)
  lateinit var deviceId: String

  /**
   * Plain column rather than a relation - the row is written by native upserts on the auth hot path.
   */
  @Column(name = "user_account_id", nullable = false)
  var userAccountId: Long = 0

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var type: UserSessionType = UserSessionType.UNKNOWN

  @Column(length = 64)
  var ip: String? = null

  @Column(name = "user_agent", length = 255)
  var userAgent: String? = null

  /**
   * Resolved from [ip] when the session is written, so the location stays the one the login was made
   * from even after the GeoIP database is updated.
   */
  @Column(name = "country_code", length = 2)
  var countryCode: String? = null

  @Column(length = 255)
  var country: String? = null

  @Column(length = 255)
  var city: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "expires_at", nullable = false)
  lateinit var expiresAt: Date

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_refreshed_at")
  var lastRefreshedAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_used_at")
  var lastUsedAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "revoked_at")
  var revokedAt: Date? = null

  @Column(name = "revoked_by_id")
  var revokedById: Long? = null

  /**
   * The impersonator, for sessions started by impersonation.
   */
  @Column(name = "acting_user_account_id")
  var actingUserAccountId: Long? = null
}
