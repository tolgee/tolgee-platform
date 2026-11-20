package io.tolgee.activity.stream

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivity.ActivityViewByRevisionsProvider
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Service
class ActivityStreamService(
  private val jdbcTemplate: JdbcTemplate,
  private val applicationContext: ApplicationContext,
  private val activityRevisionRepository: ActivityRevisionRepository,
) {
  @Transactional(readOnly = true)
  fun find(
    projectId: Long,
    filter: ActivityStreamFilter,
    pageable: Pageable,
  ): Page<ProjectActivityView> {
    val params = mutableListOf<Any>(projectId)
    val where = mutableListOf("ar.project_id = ?", "ar.batch_job_chunk_execution_id is null")
    val types = filter.types?.takeIf { it.isNotEmpty() } ?: VISIBLE_TYPES
    where += "ar.type in (${types.joinToString { "'${ActivityType.valueOf(it).name}'" }})"
    filter.authorId?.let {
      where += "ar.author_id = ?"
      params += it
    }
    filter.excludeAuthorId?.let {
      where += "ar.author_id is distinct from ?"
      params += it
    }
    filter.from?.let {
      where += "ar.timestamp >= ?"
      params += Timestamp(it)
    }
    filter.to?.let {
      where += "ar.timestamp < ?"
      params += Timestamp(it)
    }
    filter.languageIds?.takeIf { it.isNotEmpty() }?.let { ids ->
      val list = ids.joinToString { it.toString() }
      where +=
        """
        exists (
          select 1 from activity_modified_entity ame
          where ame.activity_revision_id = ar.id
            and ((ame.describing_relations -> 'language' ->> 'entityId')::bigint in ($list)
              or (ame.entity_class = 'Language' and ame.entity_id in ($list)))
        )
        """.trimIndent()
    }
    val whereSql = where.joinToString(" and ")
    val total =
      jdbcTemplate.queryForObject(
        "select count(*) from activity_revision ar where $whereSql",
        Long::class.java,
        *params.toTypedArray(),
      ) ?: 0L
    val ids =
      jdbcTemplate
        .queryForList(
          "select ar.id from activity_revision ar where $whereSql order by ar.id desc limit ? offset ?",
          Long::class.java,
          *(params + pageable.pageSize + pageable.offset).toTypedArray(),
        ).filterNotNull()
    val byId = activityRevisionRepository.findAllById(ids).associateBy { it.id }
    val revisions = ids.mapNotNull { byId[it] }
    val views = ActivityViewByRevisionsProvider(applicationContext, revisions).get()
    val viewsById = views.associateBy { it.revisionId }
    return PageImpl(ids.mapNotNull { viewsById[it] }, pageable, total)
  }

  companion object {
    private val VISIBLE_TYPES = ActivityType.entries.filter { !it.hideInList }.map { it.name }
  }
}

data class ActivityStreamFilter(
  val authorId: Long? = null,
  val excludeAuthorId: Long? = null,
  val types: List<String>? = null,
  val languageIds: List<Long>? = null,
  val from: Long? = null,
  val to: Long? = null,
)
