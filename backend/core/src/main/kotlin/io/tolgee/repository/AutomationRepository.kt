package io.tolgee.repository

import io.tolgee.model.automations.Automation
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AutomationRepository : JpaRepository<Automation?, Long?> {
  @Query(
    """
    from Automation a
      join fetch a.actions
      where a.id = :id
  """,
  )
  fun find(id: Long): Automation?

  fun deleteByIdAndProjectId(
    automationId: Long,
    projectId: Long,
  ): Long

  fun findByIdAndProjectId(
    automationId: Long,
    projectId: Long,
  ): Automation?
}
