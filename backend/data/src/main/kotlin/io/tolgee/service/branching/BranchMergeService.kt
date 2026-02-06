package io.tolgee.service.branching

interface BranchMergeService {
  fun deleteChangesByKeyIds(keyIds: Collection<Long>)
}
