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
 * One OAuth client secret of an [AppInstall]. An install may hold several live secrets at once, so a
 * new one can be issued and picked up by the app while the old one still authenticates; the old one
 * is revoked separately, once [lastUsedAt] shows nothing is using it any more.
 *
 * The plaintext is disclosed only in the response to issuing it — only [secretHash] and a short
 * display [secretPrefix] are persisted.
 */
@Entity
@Table(
  name = "app_install_secret",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_install_secret_secret_hash_unique",
      columnNames = ["secret_hash"],
    ),
  ],
  indexes = [
    Index(columnList = "app_install_id"),
  ],
)
class AppInstallSecret : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var appInstall: AppInstall

  @Column(name = "secret_hash", length = 128, nullable = false)
  lateinit var secretHash: String

  @Column(name = "secret_prefix", length = 16, nullable = false)
  lateinit var secretPrefix: String

  /**
   * When the secret was last accepted at the token endpoint. Written asynchronously and only when it
   * would move by more than [io.tolgee.service.apps.AppInstallSecretService.LAST_USED_THROTTLE_MS],
   * so it is a coarse "still in use" signal rather than an exact audit trail.
   */
  @Column(name = "last_used_at")
  var lastUsedAt: Date? = null

  @Column(name = "revoked_at")
  var revokedAt: Date? = null
}
