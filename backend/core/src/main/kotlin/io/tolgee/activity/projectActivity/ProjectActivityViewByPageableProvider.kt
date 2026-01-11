package io.tolgee.activity.projectActivity

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ProjectActivityViewByPageableProvider(
  private val applicationContext: ApplicationContext,
  private val projectId: Long,
  private val pageable: Pageable,
) {
  fun get(): Page<ProjectActivityView> {
    val revisions = getProjectActivityRevisions(projectId, pageable)
    val newContent = ActivityViewByRevisionsProvider(applicationContext, revisions.content).get()
    return PageImpl(newContent, revisions.pageable, revisions.totalElements)
  }

  private fun getProjectActivityRevisions(
    projectId: Long,
    pageable: Pageable,
  ): Page<ActivityRevision> {
    val types = ActivityType.entries.filter { !it.hideInList }
    return activityRevisionRepository.getForProject(projectId, pageable, types)
  }

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)
}
