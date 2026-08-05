package io.tolgee.model.apps

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.UserAccount
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
  name = "app_available_for_organization",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_available_for_organization_unique",
      columnNames = ["app_install_id", "organization_id"],
    ),
  ],
  indexes = [
    Index(columnList = "app_install_id"),
    Index(columnList = "organization_id"),
  ],
)
class AppAvailableForOrganization : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var appInstall: AppInstall

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var organization: Organization

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var author: UserAccount
}
