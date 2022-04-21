package io.tolgee.service

import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import io.tolgee.model.views.projectStats.ProjectStatsView
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.service.query_builders.LanguageStatsProvider
import io.tolgee.service.query_builders.ProjectStatsProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager

@Transactional
@Service
class ProjectStatsService(
  private val entityManager: EntityManager,
  private val activityRevisionRepository: ActivityRevisionRepository
) {
  fun getLanguageStats(projectId: Long): List<ProjectLanguageStatsResultView> {
    return LanguageStatsProvider(entityManager, projectId).getResult()
  }

  fun getProjectStats(projectId: Long): ProjectStatsView {
    return ProjectStatsProvider(entityManager, projectId).getResult()
  }

  fun getProjectDailyActivity(projectId: Long): Map<LocalDate, Long> {
    return activityRevisionRepository.getProjectDailyActivity(projectId).map {
      val date = LocalDate.parse(it[1] as String)
      LocalDate.from(date) to it[0] as Long
    }.toMap()
  }
}
