package io.tolgee.repository.branching

import io.tolgee.model.branching.BranchMerge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchMergeRepositoryOss : JpaRepository<BranchMerge, Long> {
  @Modifying
  @Query(
    """
    delete from BranchMerge bm
    where bm.sourceBranch.id in (select b.id from Branch b where b.project.id = :projectId)
      or bm.targetBranch.id in (select b.id from Branch b where b.project.id = :projectId)
    """,
  )
  fun deleteAllByProjectId(projectId: Long)
}
