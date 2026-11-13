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
  private val changeQueries: NotificationChangeQueries,
) {
  private val cap get() = tolgeeProperties.notifications.entityCap

  fun classify(revisionId: Long): ClassifiedRevision? {
    val header = loadHeader(revisionId) ?: return null
    val items =
      changeQueries
        .typesFor(header.batchJobType)
        .associateWith { changeQueries.entitiesInRevision(revisionId, it, cap) }
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
}
