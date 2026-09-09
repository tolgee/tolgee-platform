package io.tolgee.model.apps

import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * One entry in an app's availability set. A row with a real [organization] makes the app installable
 * by that organization; a row whose [organization] is null is the sentinel meaning the app is
 * available to every organization on the server. The owner is always implicitly available and is not
 * represented here.
 */
@Entity
@Table(
  name = "app_availability",
  uniqueConstraints = [
    UniqueConstraint(
      name = "app_availability_app_organization_unique",
      columnNames = ["app_id", "organization_id"],
    ),
  ],
  indexes = [
    Index(columnList = "app_id"),
    Index(columnList = "organization_id"),
  ],
)
class AppAvailability : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "app_id")
  lateinit var app: App

  /** Null means the sentinel row: the app is available to every organization. */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "organization_id")
  var organization: Organization? = null
}
