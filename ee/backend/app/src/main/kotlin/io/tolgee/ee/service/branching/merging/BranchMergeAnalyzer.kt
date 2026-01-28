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
  private data class KeySignature(
    val namespace: String?,
    val name: String,
  )

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
    val sourceBySignature = sourceKeys.groupBy { KeySignature(it.namespace?.name, it.name) }
    val targetBySignature = targetKeys.groupBy { KeySignature(it.namespace?.name, it.name) }
    val snapshotsBySignature = snapshots.groupBy { KeySignature(it.namespace, it.name) }

    // Fallback mapping for source keys that were deleted and recreated
    val fallbackByBranchKeyId =
      snapshotsBySignature
        .mapNotNull { (signature, snapshotGroup) ->
          if (snapshotGroup.size != 1) return@mapNotNull null
          val sourceGroup = sourceBySignature[signature] ?: return@mapNotNull null
          if (sourceGroup.size != 1) return@mapNotNull null
          val snapshot = snapshotGroup.single()
          val sourceKey = sourceGroup.single()
          snapshot.branchKeyId to sourceKey
        }.toMap()
    val fallbackSourceKeyIds = fallbackByBranchKeyId.values.map { it.id }.toSet()

    // Fallback mapping for target keys that were deleted and recreated with the same name/namespace
    val fallbackTargetByOriginalKeyId =
      snapshotsBySignature
        .mapNotNull { (signature, snapshotGroup) ->
          if (snapshotGroup.size != 1) return@mapNotNull null
          val snapshot = snapshotGroup.single()
          // Only use fallback if the original target key is actually missing (deleted)
          if (targetById.containsKey(snapshot.originalKeyId)) return@mapNotNull null
          val targetGroup = targetBySignature[signature] ?: return@mapNotNull null
          if (targetGroup.size != 1) return@mapNotNull null
          val targetKey = targetGroup.single()
          snapshot.originalKeyId to targetKey
        }.toMap()

    // Keys created after branching
    sourceKeys.forEach { key ->
      if (!snapshotByBranchKeyId.containsKey(key.id) && !fallbackSourceKeyIds.contains(key.id)) {
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
      val sourceKey = sourceById[snapshot.branchKeyId] ?: fallbackByBranchKeyId[snapshot.branchKeyId]
      val targetKey = targetById[snapshot.originalKeyId] ?: fallbackTargetByOriginalKeyId[snapshot.originalKeyId]

      when {
        sourceKey == null && targetKey == null -> {
          // deleted on both branches -> nothing to merge
        }

        sourceKey == null && targetKey != null -> {
          val targetChanged = targetKey.hasChanged(snapshot)
          if (!targetChanged) {
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
        }

        sourceKey != null && targetKey == null -> {
          val sourceChanged = sourceKey.hasChanged(snapshot)
          if (sourceChanged) {
            // target deleted key but source modified -> treat as addition back
            changes.add(
              BranchMergeChange().apply {
                branchMerge = merge
                this.sourceKey = sourceKey
                change = BranchKeyMergeChangeType.ADD
                resolution = BranchKeyMergeResolutionType.SOURCE
              },
            )
          }
        }

        sourceKey != null && targetKey != null -> {
          val sourceChanged = sourceKey.hasChanged(snapshot)
          val targetChanged = targetKey.hasChanged(snapshot)
          // Check if target key was deleted and recreated (different ID from snapshot)
          val targetRecreated = targetKey.id != snapshot.originalKeyId

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

            targetChanged || targetRecreated -> {
              // If target was recreated even without content changes, treat as UPDATE
              // This ensures the recreation is properly tracked in the merge
              if (targetRecreated) {
                changes.add(
                  BranchMergeChange().apply {
                    branchMerge = merge
                    this.sourceKey = sourceKey
                    this.targetKey = targetKey
                    change = BranchKeyMergeChangeType.UPDATE
                    resolution = BranchKeyMergeResolutionType.TARGET
                  },
                )
              }
              // Otherwise: target changed, source not -> keep target state (no change needed)
            }
          }
        }
      }
    }

    return changes
  }
}
