package io.tolgee.contributors

import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import java.util.Date

/**
 * Persists a bare [ActivityRevision] to fire the `track_project_contributor` trigger (null
 * `projectId`/`authorId` exercise its guards). Twin: `AbstractE2eDataController.recordContributorActivity`.
 */
object ContributorActivityRecorder {
  fun record(
    entityManager: EntityManager,
    currentDateProvider: CurrentDateProvider,
    projectId: Long?,
    authorId: Long?,
    at: Date,
  ) {
    val previousForcedDate = currentDateProvider.forcedDate
    currentDateProvider.forcedDate = at
    try {
      entityManager.persist(
        ActivityRevision().apply {
          this.projectId = projectId
          this.authorId = authorId
        },
      )
      entityManager.flush()
    } finally {
      currentDateProvider.forcedDate = previousForcedDate
    }
  }
}
