package io.tolgee.ee.repository.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.model.branching.BranchMerge
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BranchMergeRepository : JpaRepository<BranchMerge, Long> {
  @Query(
    """
      select bm from BranchMerge bm 
      join fetch bm.sourceBranch sb
      where sb.project.id = :projectId and bm.id = :mergeId and bm.mergedAt IS NULL
    """,
  )
  fun findActiveMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMerge?

  @Query(
    """
      select bm from BranchMerge bm 
      join fetch bm.sourceBranch sb
      join fetch bm.targetBranch tb
      left join fetch bm.changes ch
      where sb.project.id = :projectId and bm.id = :mergeId and bm.mergedAt IS NULL
    """,
  )
  fun findActiveMergeFull(
    projectId: Long,
    mergeId: Long,
  ): BranchMerge?

  @Query(
    """
      select bm from BranchMerge bm 
      join fetch bm.sourceBranch sb
      where sb.project.id = :projectId and bm.id = :mergeId
    """,
  )
  fun findMerge(
    projectId: Long,
    mergeId: Long,
  ): BranchMerge?

  @Query(
    """
    select new io.tolgee.dtos.queryResults.branching.BranchMergeView(
      bm.id,
      sb,
      tb,
      bm.mergedAt,
      case when sb.revision = bm.sourceRevision and tb.revision = bm.targetRevision then true else false end,
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.ADD then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.DELETE then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.UPDATE then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT and ch.resolution IS NULL then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT and ch.resolution IS NOT NULL then 1 end),
      (
        select count(t)
        from Task t
        where (
          t.branch.id = sb.id
          or (t.branch is null and sb.isDefault = true)
        )
          and t.state in (
            io.tolgee.model.enums.TaskState.NEW,
            io.tolgee.model.enums.TaskState.IN_PROGRESS
          )
      )
    )
    from BranchMerge bm
      join bm.sourceBranch sb
      join bm.targetBranch tb
      left join bm.changes ch
    where sb.project.id = :projectId
      and bm.id = :branchMergeId
    group by bm.id, sb, tb
    """,
  )
  fun findBranchMergeView(
    projectId: Long,
    branchMergeId: Long,
  ): BranchMergeView?

  @Query(
    value = """
    select new io.tolgee.dtos.queryResults.branching.BranchMergeView(
      bm.id,
      sb,
      tb,
      bm.mergedAt,
      case when sb.revision = bm.sourceRevision and tb.revision = bm.targetRevision then true else false end,
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.ADD then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.DELETE then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.UPDATE then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT and ch.resolution IS NULL then 1 end),
      count(case when ch.change = io.tolgee.model.enums.BranchKeyMergeChangeType.CONFLICT and ch.resolution IS NOT NULL then 1 end),
      (
        select count(t)
        from Task t
        where (
          t.branch.id = sb.id
          or (t.branch is null and sb.isDefault = true)
        )
          and t.state in (
            io.tolgee.model.enums.TaskState.NEW,
            io.tolgee.model.enums.TaskState.IN_PROGRESS
          )
      )
    )
    from BranchMerge bm
      join bm.sourceBranch sb
      join bm.targetBranch tb
      left join bm.changes ch
    where sb.project.id = :projectId
    group by bm.id, sb, tb
    order by bm.createdAt desc
    """,
    countQuery = """
    select count(bm)
    from BranchMerge bm
      join bm.sourceBranch sb
    where sb.project.id = :projectId
    """,
  )
  fun findBranchMerges(
    projectId: Long,
    pageable: Pageable,
  ): Page<BranchMergeView>

  fun findAllBySourceBranchIdOrTargetBranchId(
    sourceBranchId: Long,
    targetBranchId: Long,
  ): List<BranchMerge>
}
