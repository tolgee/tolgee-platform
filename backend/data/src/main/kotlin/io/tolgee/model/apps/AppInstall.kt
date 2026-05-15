package io.tolgee.model.apps

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
  name = "app_install",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_install_organization_id_app_id_unique",
      columnNames = ["organization_id", "app_id"],
    ),
  ],
  indexes = [
    Index(columnList = "organization_id"),
    Index(columnList = "author_id"),
  ],
)
class AppInstall : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var organization: Organization

  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var author: UserAccount

  lateinit var manifestUrl: String

  lateinit var appId: String

  lateinit var name: String

  lateinit var version: String

  lateinit var baseUrl: String

  @Column(columnDefinition = "TEXT")
  lateinit var manifestJson: String

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = Scope::class, fetch = FetchType.EAGER)
  @CollectionTable(
    name = "app_install_granted_scope",
    joinColumns = [JoinColumn(name = "app_install_id")],
  )
  @Column(name = "scope")
  var grantedScopes: MutableSet<Scope> = mutableSetOf()

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "app_install_webhook_subscription",
    joinColumns = [JoinColumn(name = "app_install_id")],
  )
  @Column(name = "event")
  var webhookSubscriptions: MutableSet<String> = mutableSetOf()

  @Column(name = "webhook_url", length = 2048)
  var webhookUrl: String? = null

  @Column(name = "client_id", length = 64, unique = true)
  var clientId: String? = null

  @Column(name = "client_secret_hash", length = 128, unique = true)
  var clientSecretHash: String? = null

  @Column(name = "client_secret_prefix", length = 16)
  var clientSecretPrefix: String? = null

  @Column(name = "webhook_secret", length = 128)
  var webhookSecret: String? = null
}
