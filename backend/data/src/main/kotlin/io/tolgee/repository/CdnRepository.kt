package io.tolgee.repository

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
  fun findAllByProjectId(projectId: Long, pageable: Pageable): Page<Cdn>
  fun getByProjectIdAndId(projectId: Long, cdnId: Long): Cdn
}
