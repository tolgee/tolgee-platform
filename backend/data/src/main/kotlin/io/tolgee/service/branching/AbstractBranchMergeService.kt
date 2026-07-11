package io.tolgee.service.branching

import io.tolgee.repository.branching.BranchMergeChangeRepositoryOss

abstract class AbstractBranchMergeService(
  protected open val branchMergeChangeRepositoryOss: BranchMergeChangeRepositoryOss,
) : BranchMergeService {
  override fun deleteChangesByKeyIds(keyIds: Collection<Long>) {
    branchMergeChangeRepositoryOss.deleteBySourceOrTargetIds(keyIds)
  }
}
