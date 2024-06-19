package io.tolgee.activity.projectActivity

import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import org.springframework.context.ApplicationContext

class ProjectActivityViewByRevisionProvider(
  private val applicationContext: ApplicationContext,
  private val revisionId: Long,
  private val onlyCountInListAbove: Int = 0,
  private val projectId: Long? = null,
) {
  fun get(): ProjectActivityView? {
    val revisions = getProjectActivityRevisions()
    val views = ActivityViewByRevisionsProvider(applicationContext, revisions, onlyCountInListAbove).get()
    return views.firstOrNull()
  }

  private fun getProjectActivityRevisions(): List<ActivityRevision> {
    val revision = activityRevisionRepository.find(projectId, revisionId)
    return revision?.let { listOf(it) } ?: listOf()
  }

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)
}
