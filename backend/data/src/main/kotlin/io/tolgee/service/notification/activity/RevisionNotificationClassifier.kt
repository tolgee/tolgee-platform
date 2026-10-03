package io.tolgee.service.notification.activity

import io.tolgee.batch.data.BatchJobType
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.notifications.NotificationType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

data class ClassifiedRevision(
  val revisionId: Long,
  val projectId: Long,
  val authorId: Long?,
  val baseLanguageId: Long,
  val items: Map<NotificationType, Map<Long?, List<Long>>>,
)

@Component
class RevisionNotificationClassifier(
  private val jdbcTemplate: JdbcTemplate,
  private val tolgeeProperties: TolgeeProperties,
) {
  private val cap get() = tolgeeProperties.notifications.entityCap

  fun classify(revisionId: Long): ClassifiedRevision? {
    val header = loadHeader(revisionId) ?: return null
    val items =
      if (header.batchJobType != null) {
        classifyBatch(revisionId, header.batchJobType)
      } else {
        classifyUserAction(revisionId, header.baseLanguageId)
      }
    return ClassifiedRevision(
      revisionId = revisionId,
      projectId = header.projectId,
      authorId = header.authorId,
      baseLanguageId = header.baseLanguageId,
      items = items.filterValues { it.isNotEmpty() },
    )
  }

  private data class Header(
    val projectId: Long,
    val authorId: Long?,
    val baseLanguageId: Long,
    val batchJobType: BatchJobType?,
  )

  private fun loadHeader(revisionId: Long): Header? =
    jdbcTemplate
      .query(
        """
        select ar.project_id, ar.author_id, p.base_language_id, bj.type
        from activity_revision ar
        join project p on p.id = ar.project_id and p.deleted_at is null
        left join tolgee_batch_job bj on bj.id = ar.batch_job_id
        where ar.id = ?
        """,
        { rs, _ ->
          Header(
            projectId = rs.getLong(1),
            authorId = rs.getLong(2).takeUnless { rs.wasNull() },
            baseLanguageId = rs.getLong(3),
            batchJobType = rs.getString(4)?.let { BatchJobType.valueOf(it) },
          )
        },
        revisionId,
      ).firstOrNull()

  private fun classifyBatch(
    revisionId: Long,
    jobType: BatchJobType,
  ): Map<NotificationType, Map<Long?, List<Long>>> {
    val type =
      when (jobType) {
        BatchJobType.MACHINE_TRANSLATE, BatchJobType.AUTO_TRANSLATE, BatchJobType.PRE_TRANSLATE_BT_TM ->
          NotificationType.AUTOMATICALLY_TRANSLATED
        BatchJobType.SET_TRANSLATIONS_STATE, BatchJobType.COPY_TRANSLATIONS, BatchJobType.CLEAR_TRANSLATIONS ->
          NotificationType.BULK_CHANGED
        else -> return emptyMap()
      }
    val samples =
      jdbcTemplate.query(
        """
        select distinct on (language_id, coalesce(ame.branch_id, 0))
               (ame.describing_relations -> 'language' ->> 'entityId')::bigint as language_id,
               ame.entity_id
        from activity_modified_entity ame
        where ame.activity_revision_id = ? and ame.entity_class = 'Translation'
        order by language_id, coalesce(ame.branch_id, 0), ame.entity_id
        """,
        { rs, _ -> rs.getLong(1) to rs.getLong(2) },
        revisionId,
      )
    return mapOf(type to samples.groupBy<Pair<Long, Long>, Long?, Long>({ it.first }, { it.second }))
  }

  private fun classifyUserAction(
    revisionId: Long,
    baseLanguageId: Long,
  ): Map<NotificationType, Map<Long?, List<Long>>> =
    mapOf(
      NotificationType.KEYS_ADDED to mapOf(null to keysAdded(revisionId)),
      NotificationType.SOURCE_CHANGED to mapOf(null to sourceChanged(revisionId, baseLanguageId)),
      NotificationType.STRINGS_TRANSLATED to translated(revisionId, baseLanguageId),
      NotificationType.STRINGS_REVIEWED to reviewed(revisionId),
    ).mapValues { (_, byLanguage) -> byLanguage.filterValues { it.isNotEmpty() } }

  private fun keysAdded(revisionId: Long): List<Long> =
    jdbcTemplate
      .queryForList(
        """
      select ame.entity_id from activity_modified_entity ame
      where ame.activity_revision_id = ? and ame.entity_class = 'Key' and ame.revision_type = 0
      order by ame.entity_id limit ?
      """,
        Long::class.java,
        revisionId,
        cap,
      ).filterNotNull()

  private fun sourceChanged(
    revisionId: Long,
    baseLanguageId: Long,
  ): List<Long> =
    jdbcTemplate
      .queryForList(
        """
      select distinct (ame.describing_relations -> 'key' ->> 'entityId')::bigint as key_id
      from activity_modified_entity ame
      where ame.activity_revision_id = ? and ame.entity_class = 'Translation' and ame.revision_type = 1
        and ame.modifications -> 'text' is not null
        and (ame.describing_relations -> 'language' ->> 'entityId')::bigint = ?
      order by key_id limit ?
      """,
        Long::class.java,
        revisionId,
        baseLanguageId,
        cap,
      ).filterNotNull()

  private fun translated(
    revisionId: Long,
    baseLanguageId: Long,
  ) = translationsByLanguage(
    revisionId,
    """
    ame.revision_type in (0, 1)
    and ame.modifications -> 'text' is not null
    and coalesce(ame.modifications -> 'text' ->> 'new', '') <> ''
    and (ame.describing_relations -> 'language' ->> 'entityId')::bigint <> $baseLanguageId
    """,
  )

  private fun reviewed(revisionId: Long) =
    translationsByLanguage(
      revisionId,
      "ame.revision_type = 1 and ame.modifications -> 'state' ->> 'new' = 'REVIEWED'",
    )

  private fun translationsByLanguage(
    revisionId: Long,
    condition: String,
  ): Map<Long?, List<Long>> {
    val rows =
      jdbcTemplate.query(
        """
        select language_id, entity_id from (
          select (ame.describing_relations -> 'language' ->> 'entityId')::bigint as language_id,
                 ame.entity_id,
                 row_number() over (
                   partition by ame.describing_relations -> 'language' ->> 'entityId' order by ame.entity_id
                 ) as rn
          from activity_modified_entity ame
          where ame.activity_revision_id = ? and ame.entity_class = 'Translation' and $condition
        ) t where rn <= ?
        """,
        { rs, _ -> rs.getLong(1) to rs.getLong(2) },
        revisionId,
        cap,
      )
    return rows.groupBy({ it.first }, { it.second })
  }
}
