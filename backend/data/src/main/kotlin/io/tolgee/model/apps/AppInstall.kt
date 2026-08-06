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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault

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
    Index(columnList = "registered_app_id"),
  ],
)
class AppInstall : StandardAuditModel() {
  /**
   * The registered [App] this is an installation of. Two organizations installing the same manifest
   * share one app and hold one install each, with their own credentials.
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "registered_app_id")
  lateinit var app: App

  /**
   * Null for a native (first-party, server-level) app: it belongs to no customer organization and
   * a server admin controls which organizations may use it via [AppAvailableForOrganization].
   */
  @ManyToOne(fetch = FetchType.LAZY)
  var organization: Organization? = null

  /**
   * Who registered the install. A historical record and a display identity — an organization's app
   * outlives the employee who created it, so nothing operational may depend on this account still
   * existing or being enabled. What the install may do comes from [grantedScopes].
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var author: UserAccount

  @Column(nullable = false)
  lateinit var manifestUrl: String

  @Column(nullable = false)
  lateinit var appId: String

  @Column(nullable = false)
  lateinit var name: String

  @Column(nullable = false)
  lateinit var version: String

  @Column(nullable = false)
  lateinit var baseUrl: String

  @Column(columnDefinition = "TEXT", nullable = false)
  lateinit var manifestJson: String

  /**
   * Blanket availability of a native app: every organization may enable it, including organizations
   * that do not exist yet. Independent of the explicit [AppAvailableForOrganization] rows — clearing
   * this flag falls back to exactly those.
   */
  @Column(name = "available_to_all_organizations", nullable = false)
  @ColumnDefault("false")
  var availableToAllOrganizations: Boolean = false

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = Scope::class, fetch = FetchType.EAGER)
  @CollectionTable(
    name = "app_install_granted_scope",
    joinColumns = [JoinColumn(name = "app_install_id")],
  )
  @Column(name = "scope", nullable = false)
  var grantedScopes: MutableSet<Scope> = mutableSetOf()

  /**
   * OAuth client id of the app's backend (machine-to-machine). It never changes; the matching
   * secrets live in [AppInstallSecret] and are rotated independently. The backend exchanges the pair
   * at the token endpoint for a short-lived install-context access token.
   */
  @Column(name = "client_id", length = 64, unique = true)
  var clientId: String? = null

  @OneToMany(mappedBy = "appInstall", fetch = FetchType.LAZY)
  var secrets: MutableList<AppInstallSecret> = mutableListOf()
}
