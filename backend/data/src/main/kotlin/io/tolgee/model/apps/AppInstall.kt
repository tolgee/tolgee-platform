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
    Index(columnList = "principal_id"),
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

  /** The organization this app is installed in. Its projects are the ones the app can be enabled for. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var organization: Organization

  /**
   * Who registered the install. A historical "created by" record only — an organization's app
   * outlives the employee who created it, so nothing operational may depend on this account still
   * existing or being enabled. What the install may do comes from [grantedScopes].
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var author: UserAccount

  /**
   * The install's own account, the identity an install-context request runs as. It is a real
   * [UserAccount] row so that everything writing a user foreign key — a comment's author, an
   * import's, a batch job's — works without the person who registered the install still being
   * around, and so that it is obvious in the data who wrote what.
   *
   * It carries no organization role and no project permission, so it grants nothing: the install's
   * capability is [grantedScopes] alone. It cannot sign in and is excluded from seats and user
   * listings — see [UserAccount.isAppPrincipal].
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "principal_id")
  lateinit var principal: UserAccount

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

  /** See [io.tolgee.dtos.apps.AppManifest.icon]; image URLs are stored already resolved. */
  @Column(name = "icon", length = 500)
  var icon: String? = null

  /** The whole manifest — far too large for activity data; the scalar fields above cover it. */
  @Column(columnDefinition = "TEXT", nullable = false)
  lateinit var manifestJson: String

  @Enumerated(EnumType.STRING)
  @ElementCollection(targetClass = Scope::class, fetch = FetchType.EAGER)
  @CollectionTable(
    name = "app_install_granted_scope",
    joinColumns = [JoinColumn(name = "app_install_id")],
  )
  @Column(name = "scope", nullable = false)
  var grantedScopes: MutableSet<Scope> = mutableSetOf()
}
