package io.tolgee.service.branching

import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.dtos.request.branching.DryRunMergeBranchRequest
import io.tolgee.dtos.request.branching.ResolveAllBranchMergeConflictsRequest
import io.tolgee.dtos.request.branching.ResolveBranchMergeConflictRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.repository.branching.BranchRepositoryOss
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class BranchServiceOssStub(
  private val branchRepository: BranchRepositoryOss,
) : BranchService {
  override fun getBranches(
    projectId: Long,
    page: Pageable,
    search: String?,
    activeOnly: Boolean?,
  ): Page<Branch> {
    return Page.empty()
  }

  override fun getActiveBranch(
    projectId: Long,
    branchId: Long,
  ): Branch {
    throw UnsupportedOperationException()
  }

  override fun getActiveBranch(
    projectId: Long,
    branchName: String,
  ): Branch {
    return branchRepository.findActiveByProjectIdAndName(projectId, branchName)
      ?: throw NotFoundException(Message.BRANCH_NOT_FOUND)
  }

  override fun getActiveOrDefault(
    projectId: Long,
    branchName: String?,
  ): Branch? {
    return branchName?.let { getActiveBranch(projectId, it) } ?: getDefaultBranch(projectId)
  }

  override fun getDefaultBranch(projectId: Long): Branch? {
    return null
  }

  override fun renameBranch(
    projectId: Long,
    branchId: Long,
    name: String,
  ): Branch {
    throw UnsupportedOperationException()
  }

  override fun setProtected(
    projectId: Long,
    branchId: Long,
    isProtected: Boolean,
  ): Branch {
    throw UnsupportedOperationException()
  }

  override fun createBranch(
    projectId: Long,
    name: String,
    originBranchId: Long,
    author: UserAccount,
  ): Branch {
    throw UnsupportedOperationException()
  }

  override fun deleteBranch(
    projectId: Long,
    branchId: Long,
  ) {
    throw UnsupportedOperationException()
  }

  override fun dryRunMerge(
    projectId: Long,
    request: DryRunMergeBranchRequest,
  ): BranchMerge {
    throw UnsupportedOperationException()
  }

  override fun getBranchMergeView(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView {
    throw UnsupportedOperationException()
  }

  override fun refreshMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMergeView {
    throw UnsupportedOperationException()
  }

  override fun getBranchMerges(
    projectId: Long,
    pageable: Pageable,
  ): Page<BranchMergeView> {
    return Page.empty()
  }

  override fun getBranchMergeConflicts(
    projectId: Long,
    branchMergeId: Long,
    pageable: Pageable,
  ): Page<BranchMergeConflictView> {
    throw UnsupportedOperationException()
  }

  override fun getBranchMergeChanges(
    projectId: Long,
    branchMergeId: Long,
    type: BranchKeyMergeChangeType?,
    pageable: Pageable,
  ): Page<BranchMergeChangeView> {
    return Page.empty()
  }

  override fun resolveConflict(
    projectId: Long,
    mergeId: Long,
    request: ResolveBranchMergeConflictRequest,
  ) {
    throw UnsupportedOperationException()
  }

  override fun resolveAllConflicts(
    projectId: Long,
    mergeId: Long,
    request: ResolveAllBranchMergeConflictsRequest,
  ) {
    throw UnsupportedOperationException()
  }

  override fun applyMerge(
    projectId: Long,
    mergeId: Long,
    deleteBranch: Boolean?,
  ) {
    throw UnsupportedOperationException()
  }

  override fun deleteMerge(
    projectId: Long,
    mergeId: Long,
  ) {
    throw UnsupportedOperationException()
  }

  override fun enableBranchingOnProject(projectId: Long) {
    throw UnsupportedOperationException()
  }
}
