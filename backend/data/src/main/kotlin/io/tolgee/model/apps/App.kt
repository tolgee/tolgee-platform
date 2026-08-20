package io.tolgee.model.apps

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
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
import org.hibernate.annotations.Type
import java.util.Date

/**
 * A published app, registered once per server and installed by any number of organizations. The
 * registering organization owns it: it holds the app-level credentials and can remove the app from
 * every organization that installed it.
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
  ],
)
class App : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var organization: Organization

  /** Whether a server admin offered the app to every organization, not only the owner. */
  @Column(nullable = false)
  @ColumnDefault("false")
  var availableToAllOrganizations: Boolean = false

  /** The `id` declared in the manifest. */
  @Column(nullable = false)
  lateinit var appId: String

  @Column(nullable = false)
  lateinit var manifestUrl: String

  @Column(nullable = false)
  lateinit var name: String

  @Column(nullable = false)
  @ColumnDefault("''")
  var version: String = ""

  @Column(nullable = false)
  lateinit var baseUrl: String

  /** An emoji, a native icon name, or an image URL served by the app's own host. */
  @Column(length = 500)
  var icon: String? = null

  /** The manifest as last read. */
  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  lateinit var manifestJson: String

  /**
   * The scopes the manifest currently requests, comma-joined. Diffed against each install's
   * granted scopes to surface pending permission requests.
   */
  @Column(columnDefinition = "TEXT", nullable = false)
  var manifestScopes: String = ""

  @Column(length = 64, nullable = false)
  lateinit var clientId: String

  /**
   * Signs outbound lifecycle deliveries, so it is stored in plaintext — the same trade-off
   * [io.tolgee.model.webhook.WebhookConfig.webhookSecret] makes. It authenticates nothing
   * towards Tolgee.
   */
  @Column(nullable = false)
  lateinit var webhookSecret: String

  @OneToMany(mappedBy = "app", fetch = FetchType.LAZY)
  var secrets: MutableList<AppSecret> = mutableListOf()

  /**
   * Access tokens issued before this moment no longer validate — set when a secret is revoked, so
   * revocation takes effect immediately instead of when the minted tokens expire.
   */
  var tokensInvalidBefore: Date? = null

  var manifestLastCheckedAt: Date? = null

  @Column(nullable = false)
  @ColumnDefault("0")
  var manifestFailureCount: Int = 0

  var manifestFirstFailedAt: Date? = null

  @Column(length = 500)
  var manifestLastError: String? = null

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  var manifestLastFailureKind: AppManifestFailureKind? = null

  var unhealthySince: Date? = null

  var unhealthyNotifiedAt: Date? = null
}
