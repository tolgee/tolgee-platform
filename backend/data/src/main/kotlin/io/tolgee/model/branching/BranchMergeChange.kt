package io.tolgee.model.branching

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.model.EntityWithId
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * Represents a change that occurred during a branch merge. This entity is used to record the nature
 * of the change, the keys involved in the merge, and the resolution applied to the change.
 *
 * The `BranchMergeChange` entity ties the branch merge process to specific changes in branch keys.
 * It encapsulates details about the source and target keys, the type of change that occurred, and
 * the resolution applied in cases of conflict.
 *
 * The change type is represented by the [BranchKeyMergeChangeType] enum, indicating whether the key
 * was added, updated, deleted, or caused a conflict. Resolution strategy can be defined
 * using the `BranchKeyMergeResolutionType` enum. If [change] is [BranchKeyMergeChangeType.CONFLICT],
 * user must solve resolution.
 */
@Entity
@Table()
@ActivityLoggedEntity
class BranchMergeChange : StandardAuditModel(), EntityWithId {

  @ManyToOne(targetEntity = BranchMerge::class)
  @JoinColumn(name = "branch_merge_id", nullable = false)
  lateinit var branchMerge: BranchMerge

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_key_id", nullable = true)
  lateinit var sourceKey: Key

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_key_id", nullable = true)
  lateinit var targetKey: Key

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  lateinit var change: BranchKeyMergeChangeType

  @Column(nullable = true)
  @Enumerated(EnumType.STRING)
  var resolution: BranchKeyMergeResolutionType? = null
}
