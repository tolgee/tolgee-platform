package io.tolgee.core.domain.project.service

import io.tolgee.model.Project
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface IQueryProjects : JpaRepository<Project, Long> {
  @Query(
    """
    from Project p
    where p.id = :projectId
      and p.deletedAt is null
    """,
  )
  fun find(projectId: Long): Project?
}
