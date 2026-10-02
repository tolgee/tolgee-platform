package io.tolgee.service.notification.activity

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import jakarta.persistence.EntityManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class GroupedNotificationWriter(
  private val jdbcTemplate: JdbcTemplate,
  private val notificationRepository: NotificationRepository,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun write(
    userId: Long,
    projectId: Long,
    originatingUserId: Long?,
    type: NotificationType,
    entityIds: List<Long>,
    digestEnabled: Boolean,
  ): Notification {
    require(type.grouped) { "Type $type is not grouped" }
    entityManager.flush()
    val now = Timestamp(currentDateProvider.date.time)
    val existingId =
      jdbcTemplate
        .queryForList(
          """
          update notification
          set email_pending = email_pending or ?, updated_at = ?, originating_user_id = ?
          where user_id = ? and project_id = ? and type = ? and seen = false
          returning id
          """,
          Long::class.java,
          digestEnabled,
          now,
          originatingUserId,
          userId,
          projectId,
          type.name,
        ).firstOrNull()

    val notificationId =
      existingId
        ?: notificationRepository
          .saveAndFlush(
            Notification().apply {
              user = entityManager.getReference(UserAccount::class.java, userId)
              project = entityManager.getReference(Project::class.java, projectId)
              originatingUser = originatingUserId?.let { entityManager.getReference(UserAccount::class.java, it) }
              this.type = type
              emailPending = digestEnabled
            },
          ).id

    addEntities(notificationId, entityIds)
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
}
