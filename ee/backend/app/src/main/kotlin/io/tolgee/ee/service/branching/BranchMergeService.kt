package io.tolgee.ee.service.branching

import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import io.tolgee.util.Logging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BranchMergeService(
  private val keyRepository: KeyRepository,
  private val branchMergeRepository: BranchMergeRepository,
  private val branchMergeChangeRepository: BranchMergeChangeRepository,
) : Logging {

  fun dryRun(branchMerge: BranchMerge, sourceBranch: Branch, targetBranch: Branch) {
    val sourceKeys = keyRepository.findAllByBranchId(sourceBranch.id)
    val targetKeys = keyRepository.findAllByBranchId(targetBranch.id)

    val sourceKeyPairs: Map<Pair<String, Long?>, Key> = sourceKeys.associateBy { Pair(it.name, it.namespace?.id) }
    val targetKeyPairs: Map<Pair<String, Long?>, Key> = targetKeys.associateBy { Pair(it.name, it.namespace?.id) }

    branchMerge.apply {
      this.changes = computeChange(this, sourceKeyPairs, targetKeyPairs)
    }
  }

  fun dryRun(sourceBranch: Branch, targetBranch: Branch): BranchMerge {
    val branchMerge = BranchMerge().apply {
      this.sourceBranch = sourceBranch
      this.targetBranch = targetBranch
      this.sourceRevision = sourceBranch.revision
      this.targetRevision = targetBranch.revision
    }
    dryRun(branchMerge, sourceBranch, targetBranch)
    branchMergeRepository.save(branchMerge)
    return branchMerge
  }

  private fun computeChange(
    merge: BranchMerge,
    sourceKeys: Map<Pair<String, Long?>, Key>,
    targetKeys: Map<Pair<String, Long?>, Key>
  ): MutableList<BranchMergeChange> {
    val resolutions = mutableListOf<BranchMergeChange>()

    val (pairs, sourceOnly, targetOnly) = diffKeysByName(sourceKeys, targetKeys)

    // compute keys existing in both branches
    pairs.forEach { (sourceKey, targetKey) ->
      val change = BranchMergeChange().apply {
        this.branchMerge = merge
        this.sourceKey = sourceKey
        this.targetKey = targetKey
      }
      decideResolution(change, merge.sourceBranch, sourceKey, targetKey)
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

  private fun decideResolution(change: BranchMergeChange, sourceBranch: Branch, sourceKey: Key, targetKey: Key) {
    // changes are compared to timestamp when the source branch was created
    val sourceBranchCreatedAt = sourceBranch.createdAt

    // if both keys were updated after the branch creation date
    if (sourceKey.modifiedAt > sourceBranchCreatedAt && targetKey.modifiedAt > sourceBranchCreatedAt) {
      // mark as conflict
      change.apply {
        change.change = BranchKeyMergeChangeType.CONFLICT
      }
      return
    }
    // if the source key was modified after the branch creation date
    if (sourceKey.modifiedAt > sourceBranchCreatedAt) {
      // accept the source key
      change.apply {
        change.change = BranchKeyMergeChangeType.UPDATE
        change.resolution = BranchKeyMergeResolutionType.SOURCE
      }
      return
    }
    // accept the target key
    if (targetKey.modifiedAt > sourceBranchCreatedAt) {
      change.apply {
        change.change = BranchKeyMergeChangeType.UPDATE
        change.resolution = BranchKeyMergeResolutionType.TARGET
      }
      return
    }
    // default behavior (no changes in either of the branches)
    change.apply {
      change.change = BranchKeyMergeChangeType.SKIP
    }
  }

  private fun diffKeysByName(
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

  fun getMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMerge {
    return branchMergeRepository.findMerge(projectId, mergeId)
      ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
  }

  fun getMergeView(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView {
    return branchMergeRepository.findBranchMergeView(projectId, mergeId)
      ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
  }

  fun getConflicts(
    projectId: Long,
    mergeId: Long,
    pageable: Pageable,
  ): Page<BranchMergeConflictView> {
    return branchMergeChangeRepository.findBranchMergeConflicts(projectId, mergeId, pageable)
  }

  fun getConflict(
    projectId: Long,
    mergeId: Long,
    changeId: Long,
  ): BranchMergeChange {
    return branchMergeChangeRepository.findConflict(projectId, mergeId, changeId)
      ?: throw NotFoundException(Message.BRANCH_MERGE_CHANGE_NOT_FOUND)
  }
}
