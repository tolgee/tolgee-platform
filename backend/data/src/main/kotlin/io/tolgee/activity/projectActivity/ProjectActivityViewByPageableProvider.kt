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
  private val branchId: Long?,
) {
  fun get(): Page<ProjectActivityView> {
    val revisions = getProjectActivityRevisions(pageable)
    val newContent =
      ActivityViewByRevisionsProvider(
        applicationContext,
        revisions.content,
        branchId,
      ).get()
    return PageImpl(newContent, revisions.pageable, revisions.totalElements)
  }

  private fun getProjectActivityRevisions(pageable: Pageable): Page<ActivityRevision> {
    val types = ActivityType.entries.filter { !it.hideInList }
    return activityRevisionRepository.getForProject(projectId, pageable, types, branchId)
  }

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)
}
