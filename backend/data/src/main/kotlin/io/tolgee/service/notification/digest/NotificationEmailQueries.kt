package io.tolgee.service.notification.digest

import io.tolgee.model.notifications.NotificationEmailTiming
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationEntitiesQueryService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Component
import java.sql.PreparedStatement
import java.sql.Timestamp

data class EmailRecipient(
  val userId: Long,
  val email: String,
)

data class PendingEmail(
  val notificationId: Long,
  val type: NotificationType,
  val updatedAt: Timestamp,
  val dueAt: Timestamp?,
)

data class PendingWork(
  val pending: PendingEmail,
  val row: DigestRow,
  val languages: List<WorkLanguage>,
)

data class WorkLanguage(
  val id: Long,
  val name: String,
  val entityCount: Int,
)

data class DueMention(
  val pending: PendingEmail,
  val commentId: Long,
  val projectId: Long,
  val projectName: String,
  val authorName: String?,
  val text: String,
  val keyId: Long,
  val keyName: String,
  val keyNamespace: String?,
  val languageId: Long,
  val languageTag: String,
  val languageName: String,
)

@Component
class NotificationEmailQueries(
  private val jdbcTemplate: JdbcTemplate,
  private val entitiesQueryService: NotificationEntitiesQueryService,
) {
  fun findDueRecipients(
    now: Timestamp,
    soonCutoff: Timestamp,
    dailyCutoff: Timestamp,
  ): List<EmailRecipient> =
    jdbcTemplate.query(
      """
      select distinct n.user_id, ua.username
      from notification n
      join user_account ua on ua.id = n.user_id and ua.deleted_at is null and ua.disabled_at is null
      join project p on p.id = n.project_id and p.deleted_at is null
      left join notification_digest_state s on s.user_id = n.user_id
      where n.seen = false and n.email_pending = true and n.email_due_at <= ?::timestamp
        and (
          n.type = any(?)
          or (
            n.type = any(?)
            and coalesce(s.frequency, 'SOON') <> 'OFF'
            and (
              s.last_digest_sent_at is null
              or s.last_digest_sent_at <= case when s.frequency = 'DAILY' then ?::timestamp else ?::timestamp end
            )
          )
        )
      """,
      PreparedStatementSetter { ps ->
        ps.setTimestamp(1, now)
        ps.setArray(2, typeNames(ps, NotificationEmailTiming.MENTION_DELAYED))
        ps.setArray(3, typeNames(ps, NotificationEmailTiming.WORK_GROUPED))
        ps.setTimestamp(4, dailyCutoff)
        ps.setTimestamp(5, soonCutoff)
      },
    ) { rs, _ -> EmailRecipient(rs.getLong(1), rs.getString(2)) }

  fun findDueMentions(
    userId: Long,
    now: Timestamp,
  ): List<DueMention> =
    jdbcTemplate.query(
      """
      select n.id, n.type, n.updated_at, n.email_due_at, c.id, p.id, p.name, ou.name, c.text,
             k.id, k.name, ns.name, l.id, l.tag, l.name
      from notification n
      join project p on p.id = n.project_id and p.deleted_at is null
      join translation_comment c on c.id = n.linked_comment_id
      join translation t on t.id = c.translation_id
      join key k on k.id = t.key_id
      left join namespace ns on ns.id = k.namespace_id
      join language l on l.id = t.language_id
      left join user_account ou on ou.id = n.originating_user_id
      where n.user_id = ? and n.seen = false and n.email_pending = true and n.email_due_at <= ?::timestamp
        and n.type = any(?)
      order by n.id
      """,
      PreparedStatementSetter { ps ->
        ps.setLong(1, userId)
        ps.setTimestamp(2, now)
        ps.setArray(3, typeNames(ps, NotificationEmailTiming.MENTION_DELAYED))
      },
    ) { rs, _ ->
      DueMention(
        pending = pendingEmail(rs.getLong(1), rs.getString(2), rs.getTimestamp(3), rs.getTimestamp(4)),
        commentId = rs.getLong(5),
        projectId = rs.getLong(6),
        projectName = rs.getString(7),
        authorName = rs.getString(8),
        text = rs.getString(9),
        keyId = rs.getLong(10),
        keyName = rs.getString(11),
        keyNamespace = rs.getString(12),
        languageId = rs.getLong(13),
        languageTag = rs.getString(14),
        languageName = rs.getString(15),
      )
    }

  fun findPendingWork(userId: Long): List<PendingWork> {
    val pending = findPendingWorkRows(userId)
    if (pending.isEmpty()) return emptyList()
    val ids = pending.map { it.pending.notificationId }
    val counts = entitiesQueryService.getCounts(ids)
    val languages = entitiesQueryService.getLanguages(ids)
    val branches = entitiesQueryService.getBranchNames(ids)
    val keySamples = entitiesQueryService.getKeySamples(ids)
    return pending.map {
      val id = it.pending.notificationId
      PendingWork(
        pending = it.pending,
        row =
          DigestRow(
            notificationId = id,
            projectId = it.projectId,
            projectName = it.projectName,
            type = it.pending.type,
            count = counts[id] ?: 0,
            languageNames = languages[id].orEmpty().map { language -> language.name },
            branchNames = branches[id].orEmpty(),
            sampleKeys = keySamples[id].orEmpty(),
          ),
        languages =
          languages[id].orEmpty().map { language ->
            WorkLanguage(language.id, language.name, language.entityCount)
          },
      )
    }
  }

  fun clearPendingAndDropEmailOnly(rows: List<PendingEmail>) {
    if (rows.isEmpty()) return
    jdbcTemplate.update(
      """
      update notification n
      set email_pending = false
      from unnest(?::bigint[], ?::timestamp[]) as l(id, updated_at)
      where n.id = l.id and n.updated_at <= l.updated_at
      """,
      loadedRowsSetter(rows),
    )
    jdbcTemplate.update(
      "delete from notification where id = any(?) and email_only = true and email_pending = false",
      PreparedStatementSetter { ps ->
        ps.setArray(1, ps.connection.createArrayOf("bigint", rows.map { it.notificationId }.toTypedArray()))
      },
    )
  }

  fun deleteIfUnchanged(rows: List<PendingEmail>) {
    if (rows.isEmpty()) return
    jdbcTemplate.update(
      """
      delete from notification n
      using unnest(?::bigint[], ?::timestamp[]) as l(id, updated_at)
      where n.id = l.id and n.updated_at <= l.updated_at
      """,
      loadedRowsSetter(rows),
    )
  }

  private fun loadedRowsSetter(rows: List<PendingEmail>) =
    PreparedStatementSetter { ps ->
      ps.setArray(1, ps.connection.createArrayOf("bigint", rows.map { it.notificationId }.toTypedArray()))
      ps.setArray(2, ps.connection.createArrayOf("timestamp", rows.map { it.updatedAt }.toTypedArray()))
    }

  fun setEmailedCounts(work: List<PendingWork>) {
    if (work.isEmpty()) return
    jdbcTemplate.update(
      """
      update notification n
      set emailed_count = l.count
      from unnest(?::bigint[], ?::int[]) as l(id, count)
      where n.id = l.id
      """,
      PreparedStatementSetter { ps ->
        ps.setArray(1, ps.connection.createArrayOf("bigint", work.map { it.pending.notificationId }.toTypedArray()))
        ps.setArray(2, ps.connection.createArrayOf("integer", work.map { it.row.count }.toTypedArray()))
      },
    )
  }

  private class PendingWorkRow(
    val pending: PendingEmail,
    val projectId: Long,
    val projectName: String,
  )

  private fun findPendingWorkRows(userId: Long): List<PendingWorkRow> =
    jdbcTemplate.query(
      """
      select n.id, n.type, n.updated_at, n.email_due_at, p.id, p.name
      from notification n
      join project p on p.id = n.project_id and p.deleted_at is null
      where n.user_id = ? and n.seen = false and n.email_pending = true
        and n.type = any(?)
      order by p.name, n.id
      """,
      PreparedStatementSetter { ps ->
        ps.setLong(1, userId)
        ps.setArray(2, typeNames(ps, NotificationEmailTiming.WORK_GROUPED))
      },
    ) { rs, _ ->
      PendingWorkRow(
        pending = pendingEmail(rs.getLong(1), rs.getString(2), rs.getTimestamp(3), rs.getTimestamp(4)),
        projectId = rs.getLong(5),
        projectName = rs.getString(6),
      )
    }

  private fun pendingEmail(
    id: Long,
    type: String,
    updatedAt: Timestamp,
    dueAt: Timestamp?,
  ) = PendingEmail(id, NotificationType.valueOf(type), updatedAt, dueAt)

  private fun typeNames(
    ps: PreparedStatement,
    timing: NotificationEmailTiming,
  ) = ps.connection.createArrayOf("varchar", NotificationType.withEmailTiming(timing).map { it.name }.toTypedArray())
}
