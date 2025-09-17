package io.tolgee.model.branching

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.Date

/**
 * Branch entity to support feature branching on keys/translations.
 *
 * Minimal PoC version:
 * - Belongs to a Project.
 * - Has name/slug for identification within a project.
 * - isDefault marks the project's default branch.
 * - isProtected can be used to block mutations.
 * - createdFromBranch references the source branch (optional), useful for merge heuristics.
 *
 * Note: Key.branch relation will be added separately; we intentionally avoid back-references here
 * to keep this change minimal and backward compatible.
 */
@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "name"),
  ],
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["project_id", "name"]),
  ]
)
@ActivityLoggedEntity
class Branch(
  @ActivityLoggedProp
  @Column(length = 200)
  var name: String = "",

  @ActivityLoggedProp
  var isDefault: Boolean = false,

  @ActivityLoggedProp
  var isProtected: Boolean = false,

  @ActivityLoggedProp
  var archivedAt: Date? = null,

) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  lateinit var project: Project

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "origin_branch_id")
  var originBranch: Branch? = null
}
