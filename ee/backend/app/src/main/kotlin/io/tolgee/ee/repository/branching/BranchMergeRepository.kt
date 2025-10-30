package io.tolgee.ee.repository.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.model.branching.BranchMerge
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BranchMergeRepository : JpaRepository<BranchMerge, Long> {

  @Query(
    """
    select new io.tolgee.dtos.queryResults.branching.BranchMergeView(
      bm.id,
      sb,
      tb,
      coalesce(sum(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.ADD then 1 else 0 end), 0),
      coalesce(sum(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.DELETE then 1 else 0 end), 0),
      coalesce(sum(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.UPDATE then 1 else 0 end), 0),
      coalesce(sum(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT then 1 else 0 end), 0)
    )
    from BranchMerge bm
      join bm.sourceBranch sb
      join bm.targetBranch tb
      left join bm.changes ch
    where sb.id = :branchId
      and sb.project.id = :projectId
      and bm.id = :branchMergeId
    group by bm.id, sb, tb
    """
  )
  fun findBranchMergeView(projectId: Long, branchId: Long, branchMergeId: Long): BranchMergeView?

  @Query(
    """
    select new io.tolgee.dtos.queryResults.branching.BranchMergeConflictView(
        bmc.id,
        bmc.sourceKey.id,
        bmc.targetKey.id,
        bmc.resolution
      )
    from BranchMergeChange bmc
    join bmc.branchMerge bm
    WHERE
      bm.id = :branchMergeId
      and bm.sourceBranch.id = :branchId
      and bmc.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT
  """
  )
  fun findBranchMergeConflicts(
    projectId: Long,
    branchId: Long,
    branchMergeId: Long,
    pageable: Pageable
  ): Page<BranchMergeConflictView>
}
