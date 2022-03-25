package io.tolgee.service

import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import io.tolgee.model.views.projectStats.ProjectStatsView
import io.tolgee.service.query_builders.LanguageStatsProvider
import io.tolgee.service.query_builders.ProjectStatsProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Transactional
@Service
class ProjectStatsService(
  private val entityManager: EntityManager,
) {
  fun getLanguageStats(projectId: Long): List<ProjectLanguageStatsResultView> {
    return LanguageStatsProvider(entityManager, projectId).getResult()
  }

  fun getProjectStats(projectId: Long): ProjectStatsView {
    return ProjectStatsProvider(entityManager, projectId).getResult()
  }
}
