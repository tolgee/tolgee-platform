package io.tolgee.repository.cdn

import io.tolgee.model.cdn.Cdn
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CdnRepository : JpaRepository<Cdn?, Long?> {
  @Query(
      """
    select count(c) = 0 from Cdn c
    where c.project.id = :projectId and c.slug = :slug
    """
  )
  fun isSlugUnique(projectId: Long, slug: String): Boolean

  @Query(
      """
    from Cdn e
    left join fetch e.automationActions
    where e.project.id in :projectId
  """,
    countQuery = """
    select count(*)
    from Cdn e
    where e.project.id in :projectId
  """
  )
  fun findAllByProjectId(projectId: Long, pageable: Pageable): Page<Cdn>

  @Query(
      """
    from Cdn e
    left join fetch e.automationActions
    where e.id = :cdnId and e.project.id = :projectId
  """
  )
  fun getByProjectIdAndId(projectId: Long, cdnId: Long): Cdn
}
