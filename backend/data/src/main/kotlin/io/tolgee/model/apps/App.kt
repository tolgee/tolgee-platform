package io.tolgee.model.apps

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault
import java.util.Date

/**
 * A published app, registered once and installed by any number of organizations. Its credentials
 * are the app's only long-lived ones: the token endpoint exchanges them plus an install id for the
 * short-lived tokens that reach an organization's data, so revoking them — which stamps
 * [tokensInvalidBefore] — cuts the app off everywhere at once.
 */
@Entity
@Table(
  name = "app",
  uniqueConstraints = [
    UniqueConstraint(name = "app_app_id_unique", columnNames = ["app_id"]),
    UniqueConstraint(name = "app_client_id_unique", columnNames = ["client_id"]),
  ],
  indexes = [
    Index(columnList = "organization_id"),
    Index(columnList = "author_id"),
  ],
)
class App : StandardAuditModel() {
  /**
   * The organization that registered the app and may administer it. Null for an app registered at
   * server level by an admin — the server itself owns it, exactly as a native [AppInstall] belongs
   * to no organization.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var author: UserAccount

  /** The `id` declared in the manifest. Unique across the whole server. */
  @Column(name = "app_id", nullable = false)
  lateinit var appId: String

  @Column(nullable = false)
  lateinit var manifestUrl: String

  @Column(nullable = false)
  lateinit var name: String

  @Column(nullable = false)
  lateinit var baseUrl: String

  /**
   * Null for an app backfilled from an install that predates this table: no app-level credential was
   * ever disclosed for it, so it has none.
   */
  @Column(name = "client_id", length = 64)
  var clientId: String? = null

  /**
   * Signs outbound lifecycle deliveries, so it is stored in plaintext — the same trade-off
   * [io.tolgee.model.webhook.WebhookConfig.webhookSecret] already makes. It authenticates
   * nothing towards Tolgee. Null on a backfilled app until the first delivery mints one.
   */
  @Column(name = "webhook_secret")
  var webhookSecret: String? = null

  @OneToMany(mappedBy = "app", fetch = FetchType.LAZY)
  var secrets: MutableList<AppSecret> = mutableListOf()

  /**
   * Access tokens issued before this moment no longer validate. Set whenever one of the app's
   * secrets is revoked, which is what makes revocation take effect immediately instead of after the
   * tokens minted from that secret expire on their own.
   *
   * Deliberately app-wide rather than per-secret: revoking is only correct once the app already
   * holds a replacement, so the only tokens this destroys are ones the app re-mints straight away.
   */
  @Column(name = "tokens_invalid_before")
  var tokensInvalidBefore: Date? = null

  @Column(name = "manifest_last_checked_at")
  var manifestLastCheckedAt: Date? = null

  /** Consecutive failed manifest checks. Reset to zero by the first successful one. */
  @Column(name = "manifest_failure_count", nullable = false)
  @ColumnDefault("0")
  var manifestFailureCount: Int = 0

  @Column(name = "manifest_first_failed_at")
  var manifestFirstFailedAt: Date? = null

  @Column(name = "manifest_last_error", length = 500)
  var manifestLastError: String? = null

  /**
   * Whether the last failure was the host not answering or the manifest it answered with no longer
   * being valid. Only [AppManifestFailureKind.UNREACHABLE] ever leads to reaping: an invalid
   * manifest means somebody is still serving it, so the app's author is reachable and can fix it.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "manifest_last_failure_kind", length = 32)
  var manifestLastFailureKind: AppManifestFailureKind? = null

  /** When the app crossed the sustained-failure threshold. Null while it is healthy. */
  @Column(name = "unhealthy_since")
  var unhealthySince: Date? = null

  @Column(name = "unhealthy_notified_at")
  var unhealthyNotifiedAt: Date? = null
}
