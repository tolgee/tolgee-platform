package io.tolgee.model.apps

import io.tolgee.model.Project
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
  name = "app_enabled_for_project",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_enabled_for_project_unique",
      columnNames = ["app_install_id", "project_id"],
    ),
  ],
  indexes = [
    Index(columnList = "app_install_id"),
    Index(columnList = "project_id"),
  ],
)
class AppEnabledForProject : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var appInstall: AppInstall

  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var author: UserAccount
}
