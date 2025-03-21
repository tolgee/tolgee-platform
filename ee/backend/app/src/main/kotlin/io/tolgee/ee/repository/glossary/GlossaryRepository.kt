package io.tolgee.ee.repository.glossary

import io.tolgee.model.glossary.Glossary
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface GlossaryRepository : JpaRepository<Glossary, Long> {
  @Query(
    """
    from Glossary where organizationOwner.id = :organizationId and id = :id and deletedAt is null
  """,
  )
  fun find(
    organizationId: Long,
    id: Long,
  ): Glossary?

  @Query(
    """
    from Glossary where organizationOwner.id = :organizationId and deletedAt is null
  """,
  )
  fun findByOrganizationId(organizationId: Long): List<Glossary>

  @Query(
    """
    delete from glossary_project gp
    using glossary g
    where gp.glossary_id = :id
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
    id: Long,
    projectId: Long,
  ): Int

  @Query(
    """
    update Glossary
    set deletedAt = :deletedAt
    where organizationOwner.id = :organizationId and id = :id and deletedAt is null
    """,
  )
  @Modifying
  fun softDelete(
    organizationId: Long,
    id: Long,
    deletedAt: Date,
  ): Int
}
