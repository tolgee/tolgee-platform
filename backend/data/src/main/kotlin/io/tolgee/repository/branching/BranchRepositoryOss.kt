package io.tolgee.repository.branching

import io.tolgee.model.branching.Branch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchRepositoryOss : JpaRepository<Branch, Long> {
  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId and b.id = :branchId and b.deletedAt IS NULL
    """,
  )
  fun findActiveByProjectIdAndId(
    projectId: Long,
    branchId: Long,
  ): Branch?

  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId and b.deletedAt IS NULL and lower(b.name) = lower(:name)
    """,
  )
  fun findActiveByProjectIdAndName(
    projectId: Long,
    name: String,
  ): Branch?

  @Query(
    """
    select b
    from Branch b
    where b.project.id = :projectId and b.deletedAt IS NULL and b.isDefault = true
    """,
  )
  fun findDefaultByProjectId(projectId: Long): Branch?

  @Modifying
  @Query(
    """
    update Branch b set b.originBranch = null
    where b.originBranch.id in (select origin.id from Branch origin where origin.project.id = :projectId)
    """,
  )
  fun detachOriginReferencesByProjectId(projectId: Long)

  fun deleteAllByProjectId(projectId: Long)
}
