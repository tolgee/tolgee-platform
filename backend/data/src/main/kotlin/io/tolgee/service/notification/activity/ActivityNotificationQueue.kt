package io.tolgee.service.notification.activity

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

data class QueuedMarker(
  val revisionId: Long,
  val attempts: Int,
)

@Component
class ActivityNotificationQueue(
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun addIfRelevant(
    revision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    if (revision.batchJobChunkExecution != null) return
    if (revision.projectId == null) return
    val relevant = modifiedEntities.keys.any { it == Key::class || it == Translation::class }
    if (!relevant) return
    add(revision.id)
  }

  fun add(revisionId: Long) {
    jdbcTemplate.update(
      """
      insert into notification_activity_queue (activity_revision_id, attempts, created_at)
      values (?, 0, ?)
      on conflict do nothing
      """,
      revisionId,
      Timestamp(currentDateProvider.date.time),
    )
  }

  fun takeBatch(
    limit: Int,
    afterRevisionId: Long? = null,
  ): List<QueuedMarker> =
    jdbcTemplate.query(
      """
      select activity_revision_id, attempts from notification_activity_queue
      where (?::bigint is null or activity_revision_id > ?)
      order by activity_revision_id limit ?
      """,
      { rs, _ -> QueuedMarker(rs.getLong(1), rs.getInt(2)) },
      afterRevisionId,
      afterRevisionId,
      limit,
    )

  fun delete(revisionId: Long) {
    jdbcTemplate.update("delete from notification_activity_queue where activity_revision_id = ?", revisionId)
  }

  fun recordFailure(
    revisionId: Long,
    maxAttempts: Int,
  ): Boolean {
    val attempts =
      jdbcTemplate.queryForObject(
        """
        update notification_activity_queue set attempts = attempts + 1
        where activity_revision_id = ? returning attempts
        """,
        Int::class.java,
        revisionId,
      ) ?: return true
    if (attempts >= maxAttempts) {
      delete(revisionId)
      return true
    }
    return false
  }

  fun findAll(): List<Long> =
    jdbcTemplate.query(
      "select activity_revision_id from notification_activity_queue order by activity_revision_id",
    ) { rs, _ -> rs.getLong(1) }
}
