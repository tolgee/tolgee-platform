package io.tolgee.service.notification

import io.tolgee.component.CurrentDateProvider
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.service.security.PermissionService
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

class ProjectMentionSetting(
  val projectId: Long,
  val projectName: String,
  val muted: Boolean,
)

@Service
class NotificationMentionMuteService(
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
  @Lazy
  private val permissionService: PermissionService,
) {
  fun getForUser(userId: Long): List<ProjectMentionSetting> =
    projectsWithMentionActivity(userId).filter { canOpen(it.projectId, userId) }

  @Transactional
  fun setForUser(
    userId: Long,
    projectId: Long,
    muted: Boolean,
  ) {
    if (!canOpen(projectId, userId)) throw NotFoundException()
    set(userId, projectId, muted)
  }

  fun mutedUserIds(
    projectId: Long,
    userIds: Collection<Long>,
  ): Set<Long> {
    if (userIds.isEmpty()) return emptySet()
    return jdbcTemplate
      .query(
        "select user_id from notification_mention_mute where project_id = ? and user_id = any(?)",
        PreparedStatementSetter { ps ->
          ps.setLong(1, projectId)
          ps.setArray(2, ps.connection.createArrayOf("bigint", userIds.toTypedArray()))
        },
        RowMapper { rs, _ -> rs.getLong(1) },
      ).toSet()
  }

  fun isMuted(
    userId: Long,
    projectId: Long,
  ): Boolean = userId in mutedUserIds(projectId, listOf(userId))

  @Transactional
  fun set(
    userId: Long,
    projectId: Long,
    muted: Boolean,
  ) {
    if (!muted) {
      jdbcTemplate.update(
        "delete from notification_mention_mute where user_id = ? and project_id = ?",
        userId,
        projectId,
      )
      return
    }
    jdbcTemplate.update(
      """
      insert into notification_mention_mute (user_id, project_id, created_at) values (?, ?, ?)
      on conflict do nothing
      """,
      userId,
      projectId,
      Timestamp(currentDateProvider.date.time),
    )
  }

  private fun projectsWithMentionActivity(userId: Long): List<ProjectMentionSetting> =
    jdbcTemplate.query(
      """
      select p.id, p.name, exists (
        select 1 from notification_mention_mute m where m.user_id = ? and m.project_id = p.id
      )
      from project p
      where p.deleted_at is null and p.id in (
        select project_id from notification_mention_mute where user_id = ?
        union
        select project_id from notification where user_id = ? and type = 'COMMENT_MENTION'
        union
        select k.project_id from translation_comment c
          join translation t on t.id = c.translation_id
          join key k on k.id = t.key_id
          where c.author_id = ?
      )
      order by lower(p.name), p.id
      """,
      { rs, _ -> ProjectMentionSetting(rs.getLong(1), rs.getString(2), rs.getBoolean(3)) },
      userId,
      userId,
      userId,
      userId,
    )

  private fun canOpen(
    projectId: Long,
    userId: Long,
  ): Boolean {
    val scopes = permissionService.getProjectPermissionScopesNoApiKey(projectId, userId) ?: return false
    return Scope.TRANSLATIONS_VIEW in scopes
  }
}
