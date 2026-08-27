package io.tolgee.service.branching

import io.tolgee.repository.branching.BranchMergeChangeRepositoryOss
import io.tolgee.repository.branching.BranchMergeRepositoryOss

abstract class AbstractBranchMergeService(
  protected open val branchMergeChangeRepositoryOss: BranchMergeChangeRepositoryOss,
  protected open val branchMergeRepositoryOss: BranchMergeRepositoryOss,
) : BranchMergeService {
  override fun deleteChangesByKeyIds(keyIds: Collection<Long>) {
    branchMergeChangeRepositoryOss.deleteBySourceOrTargetIds(keyIds)
  }

  override fun deleteChangesByProjectId(projectId: Long) {
    branchMergeChangeRepositoryOss.deleteAllByProjectId(projectId)
  }

  override fun deleteMergesByProjectId(projectId: Long) {
    branchMergeRepositoryOss.deleteAllByProjectId(projectId)
  }
}
