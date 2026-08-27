package io.tolgee.service.branching

interface BranchMergeService {
  fun deleteChangesByKeyIds(keyIds: Collection<Long>)

  fun deleteChangesByProjectId(projectId: Long)

  fun deleteMergesByProjectId(projectId: Long)
}
