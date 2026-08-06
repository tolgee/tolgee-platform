package io.tolgee.model.apps

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * A published app, registered once and installed by any number of organizations. Its credentials
 * identify and administer the app; they never grant access to anyone's data — that is what makes it
 * safe to hand them to whoever registers it. Data access goes through [AppInstall] credentials only.
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
}
