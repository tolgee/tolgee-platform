package io.tolgee.ee.service.branching

import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.util.Logging
import org.springframework.stereotype.Service

@Service
class BranchMergeService(
  private val keyRepository: KeyRepository,
) : Logging {

  fun dryRun(sourceBranch: Branch, targetBranch: Branch): BranchMerge {
    val sourceKeys = keyRepository.findAllByBranchId(sourceBranch.id)
    val targetKeys = keyRepository.findAllByBranchId(targetBranch.id)

    val sourceKeyPairs: Map<Pair<String, Long?>, Key> = sourceKeys.associateBy { Pair(it.name, it.namespace?.id) }
    val targetKeyPairs: Map<Pair<String, Long?>, Key> = targetKeys.associateBy { Pair(it.name, it.namespace?.id) }

    return BranchMerge().apply {
      this.sourceBranch = sourceBranch
      this.targetBranch = targetBranch
      this.sourceRevision = sourceBranch.revision
      this.targetRevision = targetBranch.revision
      this.changes = computeChange(this, sourceKeyPairs, targetKeyPairs)
    }
  }

  private fun computeChange(
    merge: BranchMerge,
    sourceKeys: Map<Pair<String, Long?>, Key>,
    targetKeys: Map<Pair<String, Long?>, Key>
  ): MutableList<BranchMergeChange> {
    val resolutions = mutableListOf<BranchMergeChange>()

    val (pairs, sourceOnly, targetOnly) = diffKeysByName(sourceKeys, targetKeys)
    // changes are compared to timestamp when the source branch was created
    val sourceBranchCreatedAt = merge.sourceBranch.createdAt

    // compute keys existing in both branches
    pairs.forEach { (sourceKey, targetKey) ->
      val change = BranchMergeChange().apply {
        this.branchMerge = merge
        this.sourceKey = sourceKey
        this.targetKey = targetKey
      }
      if (sourceKey.modifiedAt > sourceBranchCreatedAt && targetKey.modifiedAt > sourceBranchCreatedAt) {
        // conflict
        change.apply {
          change.change = BranchKeyMergeChangeType.CONFLICT
        }
      }
      if (sourceKey.modifiedAt > sourceBranchCreatedAt) {
        // accept the source key
        change.apply {
          change.change = BranchKeyMergeChangeType.UPDATE
          change.resolution = BranchKeyMergeResolutionType.SOURCE
        }
      }
      if (targetKey.modifiedAt > sourceBranchCreatedAt) {
        // accept the target key
        change.apply {
          change.change = BranchKeyMergeChangeType.UPDATE
          change.resolution = BranchKeyMergeResolutionType.TARGET
        }
      }
      resolutions.add(change)
    }

    // compute sourceKeys only - will be added into the target branch
    sourceOnly.forEach { sourceKey ->
      val change = BranchMergeChange().apply {
        this.branchMerge = merge
        this.sourceKey = sourceKey
      }
      change.apply {
        change.change = BranchKeyMergeChangeType.ADD
        change.resolution = BranchKeyMergeResolutionType.SOURCE
      }
      resolutions.add(change)
    }

    // compute targetKeys only - will be removed from the source branch
    targetOnly.forEach { targetKey ->
      val change = BranchMergeChange().apply {
        this.branchMerge = merge
        this.targetKey = targetKey
      }
      change.apply {
        change.change = BranchKeyMergeChangeType.DELETE
        change.resolution = BranchKeyMergeResolutionType.TARGET
      }
      resolutions.add(change)
    }

    return resolutions
  }

  fun diffKeysByName(
    sourceKeys: Map<Pair<String, Long?>, Key>,
    targetKeys: Map<Pair<String, Long?>, Key>
  ): Triple<List<Pair<Key, Key>>, List<Key>, List<Key>> {
    val same = mutableListOf<Pair<Key, Key>>()
    val sourceOnly = mutableListOf<Key>()
    val targetOnly = mutableListOf<Key>()

    val allKeyNames = (sourceKeys.keys + targetKeys.keys).toSet()

    for (name in allKeyNames) {
      val sourceKey = sourceKeys[name]
      val targetKey = targetKeys[name]

      when {
        sourceKey != null && targetKey != null -> same += sourceKey to targetKey
        sourceKey != null -> sourceOnly += sourceKey
        targetKey != null -> targetOnly += targetKey
      }
    }

    return Triple(same, sourceOnly, targetOnly)
  }
}
