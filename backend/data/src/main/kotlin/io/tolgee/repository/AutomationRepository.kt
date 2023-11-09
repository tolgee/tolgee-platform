package io.tolgee.repository

import io.tolgee.model.automations.Automation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AutomationRepository : JpaRepository<Automation?, Long?> {
  @Query(
    """
      from Automation a
      join fetch a.actions
      where a.project.id = :projectId
    """,
    countQuery = """
      select count(a) from Automation a
      where a.project.id = :projectId
    """
  )
  fun findAllInProject(projectId: Long, pageable: Pageable): Page<Automation>

  @Query(
    """
      from Automation a
      join fetch a.triggers
      where a in :automations
    """
  )
  fun fetchTriggers(automations: Iterable<Automation>): List<Automation>
  @Query(
    """
    from Automation a
      join fetch a.actions
      where a.id = :id
  """
  )
  fun find(id: Long): Automation?
  fun deleteByIdAndProjectId(automationId: Long, projectId: Long): Long
  fun findByIdAndProjectId(automationId: Long, projectId: Long): Automation?
}
