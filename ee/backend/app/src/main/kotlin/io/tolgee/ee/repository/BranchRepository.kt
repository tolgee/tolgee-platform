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
    where b.project.id = :projectId and b.archivedAt IS NULL
    order by b.isDefault desc, b.createdAt desc, b.id desc
  """
  )
  fun getAllProjectBranches(projectId: Long, page: Pageable?, search: String?): Page<Branch>

  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId and b.id = :branchId and b.archivedAt IS NULL
    """
  )
  fun findByProjectIdAndId(projectId: Long, branchId: Long): Branch?

  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId and b.archivedAt IS NULL and lower(b.name) = lower(:name)
    """
  )
  fun findByProjectIdAndName(projectId: Long, name: String): Branch?
}
