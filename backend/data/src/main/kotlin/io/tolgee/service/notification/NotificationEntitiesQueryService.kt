package io.tolgee.service.notification

import io.tolgee.dtos.response.NotificationLanguageDto
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
      select ne.notification_id, l.id, l.tag, l.name, l.flag_emoji
      from notification_entity ne
      join notification n on n.id = ne.notification_id
      join translation t on t.id = ne.entity_id
      join language l on l.id = t.language_id
      where ne.notification_id = any(?)
        and n.type in ('STRINGS_TRANSLATED', 'STRINGS_REVIEWED', 'AUTOMATICALLY_TRANSLATED', 'BULK_CHANGED')
      group by ne.notification_id, l.id, l.tag, l.name, l.flag_emoji
      order by l.tag
      """,
      notificationIds,
    ) { rs, _ ->
      rs.getLong(1) to NotificationLanguageDto(rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5))
    }.groupBy({ it.first }, { it.second })

  fun getBranchNames(notificationIds: List<Long>): Map<Long, List<String>> =
    query(
      """
      select distinct ne.notification_id, b.name
      from notification_entity ne
      join notification n on n.id = ne.notification_id
      left join translation t on t.id = ne.entity_id and n.type not in ('KEYS_ADDED', 'SOURCE_CHANGED')
      join key k on k.id = case when n.type in ('KEYS_ADDED', 'SOURCE_CHANGED') then ne.entity_id else t.key_id end
      join branch b on b.id = k.branch_id and b.is_default = false
      where ne.notification_id = any(?)
      order by b.name
      """,
      notificationIds,
    ) { rs, _ -> rs.getLong(1) to rs.getString(2) }.groupBy({ it.first }, { it.second })

  private fun <T : Any> query(
    sql: String,
    ids: List<Long>,
    mapper: RowMapper<T>,
  ): List<T> =
    jdbcTemplate.query(
      sql,
      PreparedStatementSetter { ps -> ps.setArray(1, ps.connection.createArrayOf("bigint", ids.toTypedArray())) },
      mapper,
    )
}
