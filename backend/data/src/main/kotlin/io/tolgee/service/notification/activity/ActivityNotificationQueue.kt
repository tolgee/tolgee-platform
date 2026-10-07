package io.tolgee.service.notification.activity

import io.tolgee.Metrics
import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import jakarta.annotation.PostConstruct
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

data class QueuedMarker(
  val revisionId: Long,
  val attempts: Int,
  val projectId: Long?,
)

@Component
class ActivityNotificationQueue(
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
  private val metrics: Metrics,
) {
  @PostConstruct
  fun registerMetrics() {
    metrics.registerNotificationActivityQueue(::size, ::oldestAgeSeconds)
  }

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
      select q.activity_revision_id, q.attempts, ar.project_id
      from notification_activity_queue q
      left join activity_revision ar on ar.id = q.activity_revision_id
      where (?::bigint is null or q.activity_revision_id > ?)
      order by q.activity_revision_id limit ?
      """,
      { rs, _ -> QueuedMarker(rs.getLong(1), rs.getInt(2), rs.getLong(3).takeUnless { rs.wasNull() }) },
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
      jdbcTemplate
        .query(
          """
          update notification_activity_queue set attempts = attempts + 1
          where activity_revision_id = ? returning attempts
          """,
          { rs, _ -> rs.getInt(1) },
          revisionId,
        ).firstOrNull() ?: return true
    if (attempts >= maxAttempts) {
      delete(revisionId)
      return true
    }
    return false
  }

  fun size(): Int =
    jdbcTemplate.queryForObject("select count(*) from notification_activity_queue", Int::class.java) ?: 0

  fun oldestAgeSeconds(): Long {
    val oldest =
      jdbcTemplate
        .query("select min(created_at) from notification_activity_queue") { rs, _ -> rs.getTimestamp(1) }
        .firstOrNull() ?: return 0
    return (currentDateProvider.date.time - oldest.time).coerceAtLeast(0) / 1000
  }

  fun findAll(): List<Long> =
    jdbcTemplate.query(
      "select activity_revision_id from notification_activity_queue order by activity_revision_id",
    ) { rs, _ -> rs.getLong(1) }
}
