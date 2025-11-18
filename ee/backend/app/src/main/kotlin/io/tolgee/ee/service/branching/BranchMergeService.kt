package io.tolgee.ee.service.branching

import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.ee.exceptions.BranchMergeConflictNotResolvedException
import io.tolgee.ee.repository.branching.BranchMergeChangeRepository
import io.tolgee.ee.repository.branching.BranchMergeRepository
import io.tolgee.ee.service.branching.merging.BranchMergeAnalyzer
import io.tolgee.ee.service.branching.merging.BranchMergeExecutor
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.util.Logging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BranchMergeService(
  private val branchMergeRepository: BranchMergeRepository,
  private val branchMergeChangeRepository: BranchMergeChangeRepository,
  private val branchMergeAnalyzer: BranchMergeAnalyzer,
  private val branchMergeExecutor: BranchMergeExecutor,
) : Logging {

  fun dryRun(branchMerge: BranchMerge) {
    val changes = branchMergeAnalyzer.compute(branchMerge)
    branchMerge.sourceRevision = branchMerge.sourceBranch.revision
    branchMerge.targetRevision = branchMerge.targetBranch.revision
    branchMerge.changes.clear()
    branchMerge.changes.addAll(changes)
  }

  fun dryRun(sourceBranch: Branch, targetBranch: Branch): BranchMerge {
    val branchMerge = BranchMerge().apply {
      this.sourceBranch = sourceBranch
      this.targetBranch = targetBranch
      this.sourceRevision = sourceBranch.revision
      this.targetRevision = targetBranch.revision
    }
    dryRun(branchMerge)
    branchMergeRepository.save(branchMerge)
    return branchMerge
  }

  fun refresh(branchMerge: BranchMerge) {
    val resolvedConflicts = branchMerge.changes
      .filter { it.change == BranchKeyMergeChangeType.CONFLICT && it.resolution != null }
      .associateBy { ConflictKey(it.sourceKey?.id, it.targetKey?.id) }

    dryRun(branchMerge)

    branchMerge.changes
      .filter { it.change == BranchKeyMergeChangeType.CONFLICT }
      .forEach { change ->
        resolvedConflicts[ConflictKey(change.sourceKey?.id, change.targetKey?.id)]?.resolution?.let { resolution ->
          change.resolution = resolution
        }
      }
  }

  private data class ConflictKey(val sourceKeyId: Long?, val targetKeyId: Long?)

  fun applyMerge(merge: BranchMerge) {
    try {
      branchMergeExecutor.execute(merge)
    } catch (_: BranchMergeConflictNotResolvedException) {
      throw BadRequestException(Message.BRANCH_MERGE_CONFLICTS_NOT_RESOLVED)
    }
  }

  fun getMerges(
    projectId: Long,
    pageable: Pageable,
  ): Page<BranchMergeView> {
    return branchMergeRepository.findBranchMerges(projectId, pageable)
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

  fun findMerge(projectId: Long, mergeId: Long): BranchMerge? {
    return branchMergeRepository.findMerge(projectId, mergeId)
  }

  fun deleteMerge(projectId: Long, mergeId: Long) {
    val merge = branchMergeRepository.findMerge(projectId, mergeId)
      ?: throw NotFoundException(Message.BRANCH_MERGE_NOT_FOUND)
    branchMergeRepository.delete(merge)
  }
}
