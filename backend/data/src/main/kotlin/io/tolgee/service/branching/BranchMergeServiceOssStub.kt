package io.tolgee.service.branching

import io.tolgee.repository.branching.BranchMergeChangeRepositoryOss
import org.springframework.stereotype.Service

@Service
class BranchMergeServiceOssStub(
  branchMergeChangeRepository: BranchMergeChangeRepositoryOss,
) : AbstractBranchMergeService(branchMergeChangeRepository)
