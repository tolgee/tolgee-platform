package io.tolgee.model.apps

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type

/**
 * One organization's installation of a registered [App]. All app data (manifest, urls, icon)
 * lives on the app; the install carries only what is specific to this organization — its consent.
 */
@Entity
@Table(
  name = "app_install",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_install_organization_id_app_unique",
      columnNames = ["organization_id", "registered_app_id"],
    ),
  ],
  indexes = [
    Index(columnList = "organization_id"),
    Index(columnList = "principal_id"),
  ],
)
class AppInstall : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "registered_app_id")
  lateinit var app: App

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var organization: Organization

  /**
   * The install's own account — the identity an install-context request runs as. It is a real
   * [UserAccount] row so every user foreign key (a comment's author, an import's, a batch job's)
   * works without depending on any person's account. It carries no role and no permission; the
   * install's whole capability is [grantedScopes]. See [UserAccount.isAppPrincipal].
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "principal_id")
  lateinit var principal: UserAccount

  /** What the organization consented to. Requests beyond this show as pending until approved. */
  @Type(
    EnumArrayType::class,
    parameters = [
      Parameter(
        name = EnumArrayType.SQL_ARRAY_TYPE,
        value = "varchar",
      ),
    ],
  )
  @Column(columnDefinition = "varchar[]", nullable = false)
  var grantedScopes: Array<Scope> = arrayOf()
}
