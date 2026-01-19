package io.tolgee.ee.service.branching.merging

import io.tolgee.ee.service.branching.BranchSnapshotService
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.repository.KeyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BranchMergeAnalyzer(
  private val keyRepository: KeyRepository,
  private val branchSnapshotService: BranchSnapshotService,
) {
  @Transactional
  fun compute(merge: BranchMerge): MutableList<BranchMergeChange> {
    val snapshots = branchSnapshotService.getSnapshotKeys(merge.sourceBranch.id)
    val sourceBranch = merge.sourceBranch
    val targetBranch = merge.targetBranch

    val changes = mutableListOf<BranchMergeChange>()

    val sourceKeys =
      keyRepository.findAllDetailedByBranch(
        projectId = sourceBranch.project.id,
        branchId = sourceBranch.id,
        includeOrphanDefault = sourceBranch.isDefault,
      )
    val targetKeys =
      keyRepository.findAllDetailedByBranch(
        projectId = targetBranch.project.id,
        branchId = targetBranch.id,
        includeOrphanDefault = targetBranch.isDefault,
      )

    val sourceById = sourceKeys.associateBy { it.id }
    val targetById = targetKeys.associateBy { it.id }
    val snapshotByBranchKeyId = snapshots.associateBy { it.branchKeyId }

    // Keys created after branching
    sourceKeys.forEach { key ->
      if (!snapshotByBranchKeyId.containsKey(key.id)) {
        changes.add(
          BranchMergeChange().apply {
            branchMerge = merge
            sourceKey = key
            change = BranchKeyMergeChangeType.ADD
            resolution = BranchKeyMergeResolutionType.SOURCE
          },
        )
      }
    }

    snapshots.forEach { snapshot ->
      val sourceKey = sourceById[snapshot.branchKeyId]
      val targetKey = targetById[snapshot.originalKeyId]

      when {
        sourceKey == null && targetKey == null -> {
          // deleted on both branches -> nothing to merge
        }

        sourceKey == null && targetKey != null -> {
          // source deleted key
          changes.add(
            BranchMergeChange().apply {
              branchMerge = merge
              this.targetKey = targetKey
              change = BranchKeyMergeChangeType.DELETE
              resolution = BranchKeyMergeResolutionType.SOURCE
            },
          )
        }

        sourceKey != null && targetKey == null -> {
          // target deleted key but source kept/modified -> treat as addition back
          changes.add(
            BranchMergeChange().apply {
              branchMerge = merge
              this.sourceKey = sourceKey
              change = BranchKeyMergeChangeType.ADD
              resolution = BranchKeyMergeResolutionType.SOURCE
            },
          )
        }

        sourceKey != null && targetKey != null -> {
          val sourceChanged = sourceKey.hasChanged(snapshot)
          val targetChanged = targetKey.hasChanged(snapshot)

          when {
            sourceChanged && targetChanged -> {
              if (sourceKey.isConflicting(targetKey, snapshot)) {
                changes.add(
                  BranchMergeChange().apply {
                    branchMerge = merge
                    this.sourceKey = sourceKey
                    this.targetKey = targetKey
                    change = BranchKeyMergeChangeType.CONFLICT
                  },
                )
              } else {
                changes.add(
                  BranchMergeChange().apply {
                    branchMerge = merge
                    this.sourceKey = sourceKey
                    this.targetKey = targetKey
                    change = BranchKeyMergeChangeType.UPDATE
                    resolution = BranchKeyMergeResolutionType.SOURCE
                  },
                )
              }
            }

            sourceChanged -> {
              changes.add(
                BranchMergeChange().apply {
                  branchMerge = merge
                  this.sourceKey = sourceKey
                  this.targetKey = targetKey
                  change = BranchKeyMergeChangeType.UPDATE
                  resolution = BranchKeyMergeResolutionType.SOURCE
                },
              )
            }

            targetChanged -> {
              Unit
            } // target changed, source not -> keep target state
          }
        }
      }
    }

    return changes
  }
}
