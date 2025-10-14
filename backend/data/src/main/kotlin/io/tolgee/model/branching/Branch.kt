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
import jakarta.persistence.Table
import org.springframework.boot.context.properties.bind.DefaultValue
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
  // unique indexes are created in schema.xml
  // - project_id, name where archived_at IS NULL
  // - project_id, is_default where is_default is TRUE
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

  @Column(name = "pending")
  var pending: Boolean = false,

  @Column(name = "revision")
  @DefaultValue("0")
  var revision: Int = 0

  ) : StandardAuditModel() {
  companion object {
    const val DEFAULT_BRANCH_NAME = "main"

    fun createMainBranch(project: Project): Branch {
      val branch = Branch(
        name = DEFAULT_BRANCH_NAME,
        isDefault = true,
        isProtected = true,
      )
      branch.project = project
      project.branches.add(branch)
      return branch
    }
  }

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  lateinit var project: Project

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "origin_branch_id")
  var originBranch: Branch? = null
}
