package io.tolgee.service.notification.activity

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.SEQUENCE_NAME
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationEmailTiming
import io.tolgee.model.notifications.NotificationType
import io.tolgee.util.SequenceIdProvider
import jakarta.persistence.EntityManager
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

data class GroupedNotificationWrite(
  val userId: Long,
  val projectId: Long,
  val originatingUserId: Long?,
  val type: NotificationType,
  val entityIds: List<Long>,
  val emailEnabled: Boolean,
  val revisionIds: List<Long> = emptyList(),
)

@Component
class GroupedNotificationWriter(
  private val jdbcTemplate: JdbcTemplate,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun write(
    request: GroupedNotificationWrite,
    idProvider: SequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE),
  ): Notification {
    require(
      request.type.emailTiming == NotificationEmailTiming.WORK_GROUPED,
    ) { "Type ${request.type} is not grouped work" }
    entityManager.flush()
    val notificationId = upsertUnseen(request, idProvider, Timestamp(currentDateProvider.date.time))
    addEntities(notificationId, request.entityIds)
    addRevisions(notificationId, request.revisionIds)
    entityManager.flush()
    val notification = entityManager.find(Notification::class.java, notificationId)
    entityManager.refresh(notification)
    return notification
  }

  fun entityCount(notificationId: Long): Int =
    jdbcTemplate.queryForObject(
      "select count(*) from notification_entity where notification_id = ?",
      Int::class.java,
      notificationId,
    )!!

  private fun upsertUnseen(
    request: GroupedNotificationWrite,
    idProvider: SequenceIdProvider,
    now: Timestamp,
  ): Long {
    val props = tolgeeProperties.notifications
    val newestRevisionId = request.revisionIds.maxOrNull()
    val id = jdbcTemplate.execute(ConnectionCallback { idProvider.next(it) })!!
    return jdbcTemplate.queryForObject(
      """
      insert into notification as n (
        id, created_at, updated_at, user_id, project_id, originating_user_id, type,
        seen, email_pending, email_window_started_at, email_due_at, email_only, emailed_count
      )
      values (
        ?, ?, ?, ?, ?, ?, ?,
        false, ?, case when ? then ?::timestamp end, case when ? then ?::timestamp end, false, 0
      )
      on conflict (user_id, project_id, type) where $UNSEEN_GROUPED_PREDICATE
      do update set
        email_window_started_at = case
            when excluded.email_pending and not n.email_pending then excluded.updated_at
            else n.email_window_started_at
          end,
        email_due_at = case
            when excluded.email_pending then least(
              excluded.updated_at + ?::bigint * interval '1 millisecond',
              (case when n.email_pending then coalesce(n.email_window_started_at, n.created_at)
                    else excluded.updated_at end)
                + ?::bigint * interval '1 millisecond'
            )
            else n.email_due_at
          end,
        email_pending = n.email_pending or excluded.email_pending,
        updated_at = excluded.updated_at,
        originating_user_id = case
            when ?::bigint is null
              or ?::bigint >= coalesce((select max(r) from unnest(n.activity_revision_ids) as r), 0)
              then excluded.originating_user_id
            else n.originating_user_id
          end
      returning id
      """,
      Long::class.java,
      id,
      now,
      now,
      request.userId,
      request.projectId,
      request.originatingUserId,
      request.type.name,
      request.emailEnabled,
      request.emailEnabled,
      now,
      request.emailEnabled,
      Timestamp(now.time + minOf(props.workQuietMs, props.workMaxWaitMs)),
      props.workQuietMs,
      props.workMaxWaitMs,
      newestRevisionId,
      newestRevisionId,
    )!!
  }

  private fun addEntities(
    notificationId: Long,
    entityIds: List<Long>,
  ) {
    if (entityIds.isEmpty()) return
    jdbcTemplate.update(
      """
      insert into notification_entity (notification_id, entity_id)
      select ?, id from unnest(?::bigint[]) as id
      where not exists (
        select 1 from notification_entity e where e.notification_id = ? and e.entity_id = id
      )
      limit greatest(0, ? - (select count(*) from notification_entity where notification_id = ?))
      on conflict do nothing
      """,
    ) { ps ->
      ps.setLong(1, notificationId)
      ps.setArray(2, ps.connection.createArrayOf("bigint", entityIds.toTypedArray()))
      ps.setLong(3, notificationId)
      ps.setInt(4, tolgeeProperties.notifications.entityCap)
      ps.setLong(5, notificationId)
    }
  }

  private fun addRevisions(
    notificationId: Long,
    revisionIds: List<Long>,
  ) {
    if (revisionIds.isEmpty()) return
    jdbcTemplate.update(
      """
      update notification
      set activity_revision_ids = (
        select array_agg(id order by id) from (
          select distinct id from unnest(coalesce(activity_revision_ids, '{}'::bigint[]) || ?::bigint[]) as id
          order by id desc limit ?
        ) latest
      )
      where id = ?
      """,
    ) { ps ->
      ps.setArray(1, ps.connection.createArrayOf("bigint", revisionIds.toTypedArray()))
      ps.setInt(2, MAX_REVISION_IDS)
      ps.setLong(3, notificationId)
    }
  }

  companion object {
    private const val MAX_REVISION_IDS = 200

    /** Must match the predicate of the partial unique index `notification_grouped_unseen_idx`. */
    private val UNSEEN_GROUPED_PREDICATE =
      "seen = false and type in (" +
        NotificationType.withEmailTiming(NotificationEmailTiming.WORK_GROUPED).joinToString { "'${it.name}'" } +
        ")"
  }
}
