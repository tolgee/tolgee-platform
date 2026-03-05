package io.tolgee.ee.repository.branching

import io.tolgee.model.branching.Branch
import io.tolgee.repository.branching.BranchRepositoryOss
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchRepository : BranchRepositoryOss {
  @Query(
    """
    select distinct b
    from Branch b
      left join fetch b.merges m
    where b.project.id = :projectId and b.deletedAt IS NULL
      and (:search is null or lower(b.name) like lower(concat('%', cast(:search AS text), '%')))
      and (m.id is null or m.id = (select max(m2.id) from BranchMerge m2 where m2.sourceBranch.id = b.id))
    order by b.isDefault desc, b.createdAt desc, b.id desc
  """,
  )
  fun getAllProjectBranches(
    projectId: Long,
    page: Pageable?,
    search: String?,
  ): Page<Branch>

  @Query(
    """
    select b
    from Branch b
      left join fetch b.merges m
    where b.project.id = :projectId and b.id = :branchId and b.deletedAt IS NULL
      and (m.id is null or m.id = (select max(m2.id) from BranchMerge m2 where m2.sourceBranch.id = b.id))
  """,
  )
  fun findActiveWithLatestMerge(
    projectId: Long,
    branchId: Long,
  ): Branch?

  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId and b.id = :branchId and b.deletedAt IS NULL
    """,
  )
  fun findByProjectIdAndId(
    projectId: Long,
    branchId: Long,
  ): Branch?

  @Query(
    """
    select case when count(b) > 0 then true else false end
    from Branch b
    where b.originBranch.id = :branchId and b.deletedAt IS NULL
    """,
  )
  fun existsActiveByOriginBranchId(branchId: Long): Boolean
}
