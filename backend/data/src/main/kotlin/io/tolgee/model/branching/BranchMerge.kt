package io.tolgee.model.branching

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.model.EntityWithId
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.Date

/**
 * Represents a merge operation between branches within a project. This entity stores details
 * about the source and target branches of the merge, along with their respective revision numbers.
 */
@Entity
@Table()
@ActivityLoggedEntity
class BranchMerge :
  StandardAuditModel(),
  EntityWithId {
  @ManyToOne(targetEntity = Branch::class)
  @JoinColumn(name = "source_branch_id", nullable = false)
  @NotNull
  lateinit var sourceBranch: Branch

  @ManyToOne(targetEntity = Branch::class)
  @JoinColumn(name = "target_branch_id", nullable = false)
  @NotNull
  lateinit var targetBranch: Branch

  @Column(nullable = false)
  var sourceRevision: Int = 0

  @Column(nullable = false)
  var targetRevision: Int = 0

  @Column(nullable = true)
  var mergedAt: Date? = null

  @OneToMany(
    targetEntity = BranchMergeChange::class,
    orphanRemoval = true,
    mappedBy = "branchMerge",
    cascade = [CascadeType.ALL],
  )
  var changes: MutableList<BranchMergeChange> = mutableListOf()

  val isRevisionValid: Boolean
    get() = sourceRevision == sourceBranch.revision && targetRevision == targetBranch.revision

  val isResolved: Boolean
    get() = changes.none { !it.isResolved }

  val isReadyToMerge: Boolean
    get() = isRevisionValid && isResolved

  val isMerged: Boolean
    get() = mergedAt != null
}
