package io.tolgee.model.apps

import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * One app-level client secret of an [App] — the app's only long-lived credential. The token
 * endpoint exchanges it plus an install id for the short-lived tokens that reach an organization's
 * data, so revoking it cuts the app off everywhere at once.
 *
 * The plaintext is disclosed only in the response to issuing it.
 */
@Entity
@Table(
  name = "app_secret",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_secret_secret_hash_unique",
      columnNames = ["secret_hash"],
    ),
  ],
  indexes = [
    Index(columnList = "app_id"),
  ],
)
class AppSecret : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var app: App

  @Column(name = "secret_hash", length = 128, nullable = false)
  lateinit var secretHash: String

  @Column(name = "secret_prefix", length = 16, nullable = false)
  lateinit var secretPrefix: String

  /** Last characters of the secret, shown alongside the prefix so two secrets can be told apart. */
  @Column(name = "secret_suffix", length = 8, nullable = false)
  var secretSuffix: String = ""

  @Column(name = "last_used_at")
  var lastUsedAt: Date? = null

  /**
   * When this secret stops authenticating, or null while it has no scheduled end. A rotation sets it
   * on the outgoing secret to keep it working through a grace window (so an app that copies the new
   * one by hand is not cut off); the secret is treated as dead once the time passes, without needing
   * a scheduled job to touch the row.
   */
  @Column(name = "secret_expires_at")
  var expiresAt: Date? = null

  @Column(name = "revoked_at")
  var revokedAt: Date? = null
}
