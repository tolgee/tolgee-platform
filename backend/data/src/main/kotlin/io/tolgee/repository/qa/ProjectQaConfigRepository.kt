package io.tolgee.repository.qa

import io.tolgee.model.qa.ProjectQaConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ProjectQaConfigRepository : JpaRepository<ProjectQaConfig, Long> {
  fun findByProjectId(projectId: Long): ProjectQaConfig?

  @Modifying
  @Query("DELETE FROM ProjectQaConfig c WHERE c.project.id = :projectId")
  fun deleteAllByProjectId(projectId: Long)
}
