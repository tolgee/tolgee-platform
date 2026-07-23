package io.tolgee.development.testDataBuilder

import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import java.util.Date

/** Native insert: [ActivityRevision]'s `@PrePersist` overwrites `timestamp` from [CurrentDateProvider]. */
object ContributorActivityRecorder {
  fun record(
    entityManager: EntityManager,
    currentDateProvider: CurrentDateProvider,
    projectId: Long?,
    authorId: Long?,
    at: Date? = null,
  ) {
    entityManager
      .createNativeQuery(
        """
        insert into activity_revision (id, project_id, author_id, type, "timestamp")
        values (nextval('activity_sequence'), :projectId, :authorId, 'SET_TRANSLATIONS', :timestamp)
        """,
      ).setParameter("projectId", projectId)
      .setParameter("authorId", authorId)
      .setParameter("timestamp", at ?: currentDateProvider.date)
      .executeUpdate()
  }
}
