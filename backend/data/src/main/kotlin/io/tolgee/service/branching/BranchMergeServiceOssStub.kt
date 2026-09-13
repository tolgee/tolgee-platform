package io.tolgee.service.branching

import io.tolgee.repository.branching.BranchMergeChangeRepositoryOss
import io.tolgee.repository.branching.BranchMergeRepositoryOss
import org.springframework.stereotype.Service

@Service
class BranchMergeServiceOssStub(
  branchMergeChangeRepository: BranchMergeChangeRepositoryOss,
  branchMergeRepository: BranchMergeRepositoryOss,
) : AbstractBranchMergeService(branchMergeChangeRepository, branchMergeRepository)
