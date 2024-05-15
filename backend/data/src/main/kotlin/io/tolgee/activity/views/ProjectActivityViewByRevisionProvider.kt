package io.tolgee.activity.views

import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import org.springframework.context.ApplicationContext

class ProjectActivityViewByRevisionProvider(
  private val applicationContext: ApplicationContext,
  private val revisionId: Long,
) {
  fun get(): ProjectActivityView? {
    val revisions = getProjectActivityRevisions(revisionId)
    val views = ActivityViewByRevisionsProvider(applicationContext, revisions).get()
    return views.firstOrNull()
  }

  private fun getProjectActivityRevisions(revisionId: Long): List<ActivityRevision> {
    val revision = activityRevisionRepository.findById(revisionId).orElse(null)
    return revision?.let { listOf(it) } ?: listOf()
  }

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)
}
