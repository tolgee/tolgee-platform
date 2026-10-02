package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component

@Component
class NotificationEntitiesEnhancer(
  private val jdbcTemplate: JdbcTemplate,
) : NotificationEnhancer {
  override fun enhanceNotifications(notifications: Map<Notification, NotificationModel>) {
    val grouped = notifications.filterKeys { it.type.grouped }
    if (grouped.isEmpty()) return
    val ids = grouped.keys.map { it.id }

    val counts =
      query(
        "select notification_id, count(*) from notification_entity where notification_id = any(?) " +
          "group by notification_id",
        ids,
      ) { rs, _ -> rs.getLong(1) to rs.getInt(2) }.toMap()

    val languages =
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
        ids,
      ) { rs, _ ->
        rs.getLong(1) to NotificationLanguageModel(rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5))
      }.groupBy({ it.first }, { it.second })

    val branches =
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
        ids,
      ) { rs, _ -> rs.getLong(1) to rs.getString(2) }.groupBy({ it.first }, { it.second })

    grouped.forEach { (source, target) ->
      if (source.type.countable) target.entityCount = counts[source.id] ?: 0
      if (source.type !in KEY_TYPES) target.languages = languages[source.id].orEmpty()
      target.branches = branches[source.id].orEmpty()
    }
  }

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

  companion object {
    private val KEY_TYPES = setOf(NotificationType.KEYS_ADDED, NotificationType.SOURCE_CHANGED)
  }
}
