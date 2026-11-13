package io.tolgee.service.notification

import io.tolgee.dtos.response.NotificationKeySampleDto
import io.tolgee.dtos.response.NotificationLanguageDto
import io.tolgee.model.notifications.NotificationEntityKind
import io.tolgee.model.notifications.NotificationType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service

@Service
class NotificationEntitiesQueryService(
  private val jdbcTemplate: JdbcTemplate,
) {
  fun getCounts(notificationIds: List<Long>): Map<Long, Int> =
    query(
      "select notification_id, count(*) from notification_entity where notification_id = any(?) " +
        "group by notification_id",
      notificationIds,
    ) { rs, _ -> rs.getLong(1) to rs.getInt(2) }.toMap()

  fun getLanguages(notificationIds: List<Long>): Map<Long, List<NotificationLanguageDto>> =
    query(
      """
      select ne.notification_id, l.id, l.tag, l.name, l.flag_emoji, count(*)
      from notification_entity ne
      join notification n on n.id = ne.notification_id
      join translation t on t.id = ne.entity_id
      join language l on l.id = t.language_id
      where ne.notification_id = any(?)
        and n.type = any(?)
      group by ne.notification_id, l.id, l.tag, l.name, l.flag_emoji
      order by l.tag
      """,
      notificationIds,
      typeNames(NotificationEntityKind.TRANSLATION),
    ) { rs, _ ->
      rs.getLong(1) to
        NotificationLanguageDto(rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6))
    }.groupBy({ it.first }, { it.second })

  fun getBranchNames(notificationIds: List<Long>): Map<Long, List<String>> =
    query(
      """
      select distinct ne.notification_id, b.name
      from notification_entity ne
      join notification n on n.id = ne.notification_id and ne.notification_id = any(?)
      left join translation t on t.id = ne.entity_id and n.type = any(?)
      join key k on k.id = case when n.type = any(?) then ne.entity_id else t.key_id end
      join branch b on b.id = k.branch_id and b.is_default = false
      order by b.name
      """,
      notificationIds,
      typeNames(NotificationEntityKind.TRANSLATION),
      typeNames(NotificationEntityKind.KEY),
    ) { rs, _ -> rs.getLong(1) to rs.getString(2) }.groupBy({ it.first }, { it.second })

  fun getKeySamples(notificationIds: List<Long>): Map<Long, List<NotificationKeySampleDto>> =
    query(
      """
      select notification_id, key_id, key_name, namespace, base_text from (
        select ne.notification_id, k.id as key_id, k.name as key_name, ns.name as namespace, bt.text as base_text,
               row_number() over (partition by ne.notification_id order by ne.entity_id) as rn
        from notification_entity ne
        join notification n on n.id = ne.notification_id
        join project p on p.id = n.project_id
        join key k on k.id = ne.entity_id and k.project_id = n.project_id
        left join namespace ns on ns.id = k.namespace_id
        left join translation bt on bt.key_id = k.id and bt.language_id = p.base_language_id
        where ne.notification_id = any(?)
          and n.type = any(?)
      ) samples
      where rn <= $KEY_SAMPLE_SIZE
      order by notification_id, rn
      """,
      notificationIds,
      arrayOf(NotificationType.KEYS_ADDED.name),
    ) { rs, _ ->
      rs.getLong(1) to NotificationKeySampleDto(rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5))
    }.groupBy({ it.first }, { it.second })

  private fun typeNames(kind: NotificationEntityKind) =
    NotificationType.withEntityKind(kind).map { it.name }.toTypedArray()

  private fun <T : Any> query(
    sql: String,
    ids: List<Long>,
    vararg typeNameArrays: Array<String>,
    mapper: RowMapper<T>,
  ): List<T> =
    jdbcTemplate.query(
      sql,
      PreparedStatementSetter { ps ->
        ps.setArray(1, ps.connection.createArrayOf("bigint", ids.toTypedArray()))
        typeNameArrays.forEachIndexed { index, names ->
          ps.setArray(index + 2, ps.connection.createArrayOf("varchar", names))
        }
      },
      mapper,
    )

  companion object {
    const val KEY_SAMPLE_SIZE = 2
  }
}
