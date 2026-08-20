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
 * One app-level client secret of an [App] — the app's only long-lived credential, exchanged at the
 * token endpoint for short-lived install tokens. The plaintext is disclosed only in the response
 * to issuing it; only the hash is stored.
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

  @Column(length = 128, nullable = false)
  lateinit var secretHash: String

  /** How the secret is identified everywhere it is shown: its start and end, e.g. `tgpubs_ab…yz`. */
  @Column(length = 32, nullable = false)
  lateinit var name: String

  var lastUsedAt: Date? = null

  /**
   * When this secret stops authenticating, or null while it has no scheduled end. A rotation sets
   * it on the outgoing secret to keep it working through a grace window; past the time the secret
   * is treated as dead wherever it is read — no job touches the row.
   */
  var expiresAt: Date? = null

  var revokedAt: Date? = null
}
