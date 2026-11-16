package io.tolgee.service.notification.activity

import io.tolgee.batch.data.BatchJobType
import io.tolgee.model.notifications.NotificationType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Component

@Component
class NotificationChangeQueries(
  private val jdbcTemplate: JdbcTemplate,
) {
  fun typesFor(batchJobType: BatchJobType?): List<NotificationType> {
    return MATCHERS.filterValues { batchJobType in it.sources }.keys.toList()
  }

  /**
   * Entity ids of [type] changed in the revision, grouped by language id (`null` for key-level types).
   */
  fun entitiesInRevision(
    revisionId: Long,
    type: NotificationType,
    cap: Int,
  ): Map<Long?, List<Long>> {
    val matcher = MATCHERS[type] ?: return emptyMap()
    if (!matcher.byLanguage) return keyLevelEntities(revisionId, matcher, cap)
    if (matcher.oneSamplePerLanguage) return byLanguage(samplePerLanguageSql(matcher), revisionId)
    return byLanguage(cappedPerLanguageSql(matcher), revisionId, cap)
  }

  private fun keyLevelEntities(
    revisionId: Long,
    matcher: Matcher,
    cap: Int,
  ): Map<Long?, List<Long>> {
    val ids =
      jdbcTemplate.queryForList(
        """
        select distinct ${matcher.entityId} as entity_id
        from $FROM
        where ar.id = ? and ${matcher.condition}
        order by entity_id limit ?
        """,
        Long::class.java,
        revisionId,
        cap,
      )
    if (ids.isEmpty()) return emptyMap()
    return mapOf<Long?, List<Long>>(null to ids.filterNotNull())
  }

  private fun byLanguage(
    sql: String,
    vararg args: Any,
  ): Map<Long?, List<Long>> =
    jdbcTemplate
      .query(sql, { rs, _ -> rs.getLong(1) to rs.getLong(2) }, *args)
      .groupBy({ it.first }, { it.second })

  private fun samplePerLanguageSql(matcher: Matcher) =
    """
    select distinct on (language_id, coalesce(ame.branch_id, 0))
           $LANGUAGE_ID as language_id, ${matcher.entityId} as entity_id
    from $FROM
    where ar.id = ? and ${matcher.condition}
    order by language_id, coalesce(ame.branch_id, 0), ame.entity_id
    """

  private fun cappedPerLanguageSql(matcher: Matcher) =
    """
    select language_id, entity_id from (
      select $LANGUAGE_ID as language_id,
             ${matcher.entityId} as entity_id,
             row_number() over (partition by $LANGUAGE_ID order by ${matcher.entityId}) as rn
      from $FROM
      where ar.id = ? and ${matcher.condition}
    ) t where rn <= ?
    """

  fun keyIds(
    projectId: Long,
    type: NotificationType,
    revisionIds: List<Long>,
    languageIds: List<Long>,
    limit: Int,
  ): List<Long> {
    val matcher = MATCHERS[type] ?: return emptyList()
    if (revisionIds.isEmpty() || (matcher.byLanguage && languageIds.isEmpty())) return emptyList()
    return jdbcTemplate.query(
      """
      select distinct ${matcher.keyId} as key_id
      from $FROM
      where ar.project_id = ? and ar.id = any(?) and ${matcher.condition} ${languageCondition(matcher)}
      order by key_id limit ?
      """,
      PreparedStatementSetter { ps ->
        var index = 1
        ps.setLong(index++, projectId)
        ps.setArray(index++, ps.connection.createArrayOf("bigint", revisionIds.toTypedArray()))
        if (matcher.byLanguage) {
          ps.setArray(index++, ps.connection.createArrayOf("bigint", languageIds.toTypedArray()))
        }
        ps.setInt(index, limit)
      },
      { rs, _ -> rs.getLong(1) },
    )
  }

  private fun languageCondition(matcher: Matcher): String {
    if (!matcher.byLanguage) return ""
    return "and $LANGUAGE_ID = any(?)"
  }

  private class Matcher(
    condition: String,
    val entityId: String,
    val keyId: String,
    val byLanguage: Boolean,
    val sources: Set<BatchJobType?> = DIRECT_AND_BULK_EDITS,
    val oneSamplePerLanguage: Boolean = false,
  ) {
    val condition = "($condition) and ${sourceCondition()}"

    private fun sourceCondition(): String {
      val jobTypes = sources.filterNotNull()
      val alternatives =
        listOfNotNull(
          "bj.type is null".takeIf { null in sources },
          "bj.type in (${jobTypes.joinToString { "'${it.name}'" }})".takeIf { jobTypes.isNotEmpty() },
        )
      return alternatives.joinToString(" or ", prefix = "(", postfix = ")")
    }
  }

  companion object {
    private const val FROM = """
      activity_modified_entity ame
      join activity_revision ar on ar.id = ame.activity_revision_id
      join project p on p.id = ar.project_id
      left join tolgee_batch_job bj on bj.id = ar.batch_job_id
    """
    private const val LANGUAGE_ID = "(ame.describing_relations -> 'language' ->> 'entityId')::bigint"
    private const val KEY_OF_TRANSLATION = "(ame.describing_relations -> 'key' ->> 'entityId')::bigint"
    private const val IS_TRANSLATION = "ame.entity_class = 'Translation'"

    /** `null` stands for a change made directly, outside of any batch job. Must be declared before [MATCHERS]. */
    private val DIRECT_AND_BULK_EDITS: Set<BatchJobType?> =
      setOf(
        null,
        BatchJobType.SET_TRANSLATIONS_STATE,
        BatchJobType.COPY_TRANSLATIONS,
        BatchJobType.CLEAR_TRANSLATIONS,
      )

    private val MATCHERS =
      mapOf(
        NotificationType.KEYS_ADDED to
          Matcher(
            condition = "ame.entity_class = 'Key' and ame.revision_type = 0",
            entityId = "ame.entity_id",
            keyId = "ame.entity_id",
            byLanguage = false,
          ),
        NotificationType.SOURCE_CHANGED to
          Matcher(
            condition =
              "$IS_TRANSLATION and ame.revision_type = 1 and ame.modifications -> 'text' is not null " +
                "and $LANGUAGE_ID = p.base_language_id",
            entityId = KEY_OF_TRANSLATION,
            keyId = KEY_OF_TRANSLATION,
            byLanguage = false,
          ),
        NotificationType.STRINGS_TRANSLATED to
          Matcher(
            condition =
              "$IS_TRANSLATION and ame.revision_type in (0, 1) and ame.modifications -> 'text' is not null " +
                "and coalesce(ame.modifications -> 'text' ->> 'new', '') <> '' " +
                "and $LANGUAGE_ID <> p.base_language_id",
            entityId = "ame.entity_id",
            keyId = KEY_OF_TRANSLATION,
            byLanguage = true,
          ),
        NotificationType.STRINGS_REVIEWED to
          Matcher(
            condition =
              "$IS_TRANSLATION and ame.revision_type = 1 and ame.modifications -> 'state' ->> 'new' = 'REVIEWED'",
            entityId = "ame.entity_id",
            keyId = KEY_OF_TRANSLATION,
            byLanguage = true,
          ),
        NotificationType.AUTOMATICALLY_TRANSLATED to
          Matcher(
            condition = IS_TRANSLATION,
            entityId = "ame.entity_id",
            keyId = KEY_OF_TRANSLATION,
            byLanguage = true,
            sources =
              setOf(BatchJobType.MACHINE_TRANSLATE, BatchJobType.AUTO_TRANSLATE, BatchJobType.PRE_TRANSLATE_BT_TM),
            oneSamplePerLanguage = true,
          ),
      )
  }
}
