package io.tolgee.model.apps

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault
import java.util.Date

/**
 * The record of one lifecycle delivery attempt sequence — what an owner reads to find out why their
 * app never got its credentials.
 *
 * It holds no payload. The payload of a registration, an install or a rotation carries a bearer
 * credential in plaintext, and those are only ever stored hashed; persisting one here so a restarted
 * server could resume the retry would put every undelivered secret at rest in the database. The
 * payload therefore lives only in the sending process, and a delivery that outlives that process is
 * abandoned rather than resumed.
 */
@Entity
@Table(
  name = "app_delivery",
  indexes = [
    Index(columnList = "app_id"),
    Index(columnList = "organization_id"),
  ],
)
class AppDelivery : StandardAuditModel() {
  /**
   * Null once the app is gone. An uninstalled delivery outlives the app it announces the removal of,
   * which is exactly the delivery an owner most needs to see the outcome of.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  var app: App? = null

  /** The manifest id, kept so a delivery stays attributable after [app] is deleted. */
  @Column(name = "app_identifier", nullable = false)
  lateinit var appIdentifier: String

  /** The organization the event concerns; null for app-level events. */
  @ManyToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", length = 64, nullable = false)
  lateinit var eventType: AppLifecycleEventType

  @Column(name = "target_url", nullable = false)
  lateinit var targetUrl: String

  @Column(nullable = false)
  @ColumnDefault("0")
  var attempts: Int = 0

  @Column(name = "last_attempt_at")
  var lastAttemptAt: Date? = null

  @Column(name = "last_error", length = 500)
  var lastError: String? = null

  @Column(name = "delivered_at")
  var deliveredAt: Date? = null

  /** When retrying stopped without success. */
  @Column(name = "abandoned_at")
  var abandonedAt: Date? = null
}
