package io.tolgee.ee.repository

import io.tolgee.model.branching.Branch
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchRepository : JpaRepository<Branch, Long> {
  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId
    order by b.isDefault desc, b.archivedAt desc nulls first, b.createdAt desc, b.id desc
  """
  )
  fun getAllProjectBranches(projectId: Long, page: Pageable?, search: String?): Page<Branch>

  fun findByProjectIdAndId(projectId: Long, branchId: Long): Branch?

  fun findByProjectIdAndName(projectId: Long, name: String): Branch?
}
