package io.tolgee.repository.qa

import io.tolgee.model.qa.ProjectQaConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ProjectQaConfigRepository : JpaRepository<ProjectQaConfig, Long> {
  fun findByProjectId(projectId: Long): ProjectQaConfig?
}
