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
 * One app-level client secret of an [App], shaped like [AppInstallSecret] so a rotation works the
 * same way at both layers. It lives in its own table rather than sharing that one: an operator
 * reading `app_install_secret` must be looking at exactly the credentials that reach a tenant's
 * data, and a union table would mix the two layers in every row, index and query.
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

  @Column(name = "last_used_at")
  var lastUsedAt: Date? = null

  @Column(name = "revoked_at")
  var revokedAt: Date? = null
}
