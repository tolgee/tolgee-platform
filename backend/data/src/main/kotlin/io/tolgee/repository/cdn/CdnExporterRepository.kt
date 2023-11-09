package io.tolgee.repository.cdn

import io.tolgee.model.cdn.CdnExporter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CdnExporterRepository : JpaRepository<CdnExporter?, Long?> {
  @Query(
    """
    select count(c) = 0 from CdnExporter c
    where c.project.id = :projectId and c.slug = :slug
    """
  )
  fun isSlugUnique(projectId: Long, slug: String): Boolean

  @Query(
    """
    from CdnExporter e
    left join fetch e.automationActions
    where e.project.id in :projectId
  """,
    countQuery = """
    select count(*)
    from CdnExporter e
    where e.project.id in :projectId
  """
  )
  fun findAllByProjectId(projectId: Long, pageable: Pageable): Page<CdnExporter>

  @Query(
    """
    from CdnExporter e
    left join fetch e.automationActions
    where e.id = :cdnId and e.project.id = :projectId
  """
  )
  fun getByProjectIdAndId(projectId: Long, cdnId: Long): CdnExporter
}
