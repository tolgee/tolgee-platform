package io.tolgee.repository.contentDelivery

import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ContentDeliveryConfigRepository : JpaRepository<ContentDeliveryConfig?, Long?> {
  @Query(
    """
    select count(c) = 0 from ContentDeliveryConfig c
    where c.slug = :slug
    """,
  )
  fun isSlugUnique(slug: String): Boolean

  @Query(
    """
    from ContentDeliveryConfig e
    left join fetch e.automationActions
    where e.project.id in :projectId
  """,
    countQuery = """
    select count(*)
    from ContentDeliveryConfig e
    where e.project.id in :projectId
  """,
  )
  fun findAllByProjectId(
    projectId: Long,
    pageable: Pageable,
  ): Page<ContentDeliveryConfig>

  @Query(
    """
    from ContentDeliveryConfig e
    left join fetch e.automationActions
    where e.id = :contentDeliveryConfigId and e.project.id = :projectId
  """,
  )
  fun getByProjectIdAndId(
    projectId: Long,
    contentDeliveryConfigId: Long,
  ): ContentDeliveryConfig

  fun countByProject(project: Project): Int
}
