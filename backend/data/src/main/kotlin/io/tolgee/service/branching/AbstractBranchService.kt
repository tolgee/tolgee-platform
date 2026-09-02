package io.tolgee.service.branching

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.branching.Branch
import io.tolgee.repository.branching.BranchRepositoryOss
import io.tolgee.repository.branching.KeyMetaSnapshotRepository
import io.tolgee.repository.branching.KeySnapshotRepositoryOss
import io.tolgee.repository.branching.TranslationSnapshotRepository
import io.tolgee.repository.contentDelivery.ContentDeliveryConfigRepository

abstract class AbstractBranchService(
  protected open val branchRepository: BranchRepositoryOss,
  protected open val branchMergeService: BranchMergeService,
  protected open val contentDeliveryConfigRepository: ContentDeliveryConfigRepository,
  protected open val keySnapshotRepository: KeySnapshotRepositoryOss,
  protected open val translationSnapshotRepository: TranslationSnapshotRepository,
  protected open val keyMetaSnapshotRepository: KeyMetaSnapshotRepository,
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

  /**
   * Everything referencing the project's branches goes first — none of those FKs has an `ON DELETE`
   * action. Keys, imports, tasks and language stats also FK branch, so the caller still has to remove
   * those before calling this.
   */
  override fun deleteAllByProjectId(projectId: Long) {
    contentDeliveryConfigRepository.detachBranchByProjectId(projectId)
    branchMergeService.deleteChangesByProjectId(projectId)
    translationSnapshotRepository.deleteAllByProjectId(projectId)
    keyMetaSnapshotRepository.deleteAllByProjectId(projectId)
    keySnapshotRepository.deleteAllByProjectId(projectId)
    branchMergeService.deleteMergesByProjectId(projectId)
    branchRepository.detachOriginReferencesByProjectId(projectId)
    return branchRepository.deleteAllByProjectId(projectId)
  }
}
