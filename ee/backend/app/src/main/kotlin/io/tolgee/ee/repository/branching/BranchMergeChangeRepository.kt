package io.tolgee.ee.repository.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeChangeView
import io.tolgee.dtos.queryResults.branching.BranchMergeConflictView
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.repository.branching.BranchMergeChangeRepositoryOss
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface BranchMergeChangeRepository : BranchMergeChangeRepositoryOss {
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
      bm.id = :mergeId
      and bm.sourceBranch.project.id = :projectId
      and bmc.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT
    order by bmc.id asc
  """,
  )
  fun findBranchMergeConflicts(
    projectId: Long,
    mergeId: Long,
    pageable: Pageable,
  ): Page<BranchMergeConflictView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.branching.BranchMergeChangeView(
        bmc.id,
        bmc.change,
        bmc.resolution,
        bmc.sourceKey.id,
        bmc.targetKey.id
      )
    from BranchMergeChange bmc
    join bmc.branchMerge bm
    where bm.id = :mergeId
      and bm.sourceBranch.project.id = :projectId
      and (:type is null or bmc.change = :type)
    order by bmc.id asc
    """,
  )
  fun findBranchMergeChanges(
    projectId: Long,
    mergeId: Long,
    type: BranchKeyMergeChangeType?,
    pageable: Pageable,
  ): Page<BranchMergeChangeView>

  @Query(
    """
      select bmc from BranchMergeChange bmc 
      join bmc.branchMerge bm
      join bm.sourceBranch sb 
      where sb.project.id = :projectId 
        and bm.id = :mergeId 
        and bmc.id = :changeId
        and bm.mergedAt IS NULL
    """,
  )
  fun findActiveMergeConflict(
    projectId: Long,
    mergeId: Long,
    changeId: Long,
  ): BranchMergeChange?

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    """
    update BranchMergeChange bmc
      set bmc.resolution = :resolution
    where bmc.branchMerge.id = :mergeId
      and bmc.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT
    """,
  )
  fun resolveAllConflicts(
    mergeId: Long,
    resolution: BranchKeyMergeResolutionType,
  ): Int

  @Query(
    """
    select new io.tolgee.dtos.queryResults.branching.BranchMergeChangeView(
        bmc.id,
        bmc.change,
        bmc.resolution,
        bmc.sourceKey.id,
        bmc.targetKey.id
      )
    from BranchMergeChange bmc
    join bmc.branchMerge bm
    where bm.id = :mergeId
      and bm.sourceBranch.project.id = :projectId
      and bmc.id = :changeId
    """,
  )
  fun findBranchMergeChangeById(
    projectId: Long,
    mergeId: Long,
    changeId: Long,
  ): BranchMergeChangeView?
}
