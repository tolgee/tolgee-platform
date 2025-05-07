package io.tolgee.ee.repository.glossary

import io.tolgee.model.glossary.Glossary
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface GlossaryRepository : JpaRepository<Glossary, Long> {
  // TODO: rework - use only glossary id for query, check organization id in service?

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and id = :glossaryId
      and deletedAt is null
  """,
  )
  fun find(
    organizationId: Long,
    glossaryId: Long,
  ): Glossary?

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and deletedAt is null
  """,
  )
  fun findByOrganizationId(organizationId: Long): List<Glossary>

  @Query(
    """
    from Glossary
    where organizationOwner.id = :organizationId
      and organizationOwner.deletedAt is null
      and deletedAt is null
      and (:search is null or lower(name) like lower(concat('%', cast(:search as text), '%')))
  """,
  )
  fun findByOrganizationIdPaged(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Glossary>

  @Query(
    """
    delete from glossary_project gp
    using glossary g
    where gp.glossary_id = :glossaryId
      and gp.project_id = :projectId 
      and gp.glossary_id = g.id
      and g.organization_owner_id = :organizationId
      and g.deleted_at is null
    """,
    nativeQuery = true,
  )
  @Modifying
  fun unassignProject(
    organizationId: Long,
    glossaryId: Long,
    projectId: Long,
  ): Int

  @Query(
    """
    update Glossary
    set deletedAt = :deletedAt
    where organizationOwner.id = :organizationId
      and id = :glossaryId
      and deletedAt is null
    """,
  )
  @Modifying
  fun softDelete(
    organizationId: Long,
    glossaryId: Long,
    deletedAt: Date,
  ): Int
}
