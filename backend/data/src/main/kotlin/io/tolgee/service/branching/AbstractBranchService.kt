package io.tolgee.service.branching

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.branching.Branch
import io.tolgee.repository.branching.BranchRepositoryOss

abstract class AbstractBranchService(
  protected open val branchRepository: BranchRepositoryOss,
  protected open val branchMergeService: BranchMergeService,
) : BranchService {
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

  override fun getActiveNonDefaultBranch(
    projectId: Long,
    branchName: String?,
  ): Branch? {
    return branchName?.let { getActiveBranch(projectId, branchName).takeUnless { it.isDefault } }
  }

  override fun getDefaultBranch(projectId: Long): Branch? {
    return branchRepository.findDefaultByProjectId(projectId)
  }

  override fun deleteAllByProjectId(projectId: Long) {
    return branchRepository.deleteAllByProjectId(projectId)
  }

  override fun deleteBranchMergeChangesByKeyIds(keyIds: Set<Long>) {
  }
}
