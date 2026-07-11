package io.tolgee.service.queryBuilders

import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import jakarta.persistence.EntityManager

class LanguageStatsProvider(
  private val entityManager: EntityManager,
  private val projectId: Long,
  private val branchId: Long?,
) {
  fun getResultForSingleProject(): List<ProjectLanguageStatsResultView> {
    val branchCondition =
      if (branchId == null) {
        "(k.branch_id IS NULL OR b.is_default = true)"
      } else {
        "b.id = :branchId"
      }

    val query =
      entityManager
        .createNativeQuery(
          """
        WITH key_base_words AS MATERIALIZED (
          SELECT k.id AS key_id, t_base.word_count
          FROM key k
          LEFT JOIN branch b ON b.id = k.branch_id
          LEFT JOIN translation t_base ON k.id = t_base.key_id
            AND t_base.language_id = (SELECT base_language_id FROM project WHERE id = :projectId)
          WHERE k.project_id = :projectId
            AND $branchCondition
            AND k.deleted_at IS NULL
        ),
        lang_stats AS MATERIALIZED (
          SELECT
            t.language_id,
            COUNT(CASE WHEN t.state = 1 THEN 1 END)                        AS translated_keys,
            COALESCE(SUM(CASE WHEN t.state = 1 THEN kw.word_count END), 0) AS translated_words,
            COUNT(CASE WHEN t.state = 2 THEN 1 END)                        AS reviewed_keys,
            COALESCE(SUM(CASE WHEN t.state = 2 THEN kw.word_count END), 0) AS reviewed_words
          FROM key_base_words kw
          JOIN translation t ON kw.key_id = t.key_id AND t.state IN (1, 2)
          JOIN language l ON t.language_id = l.id AND l.project_id = :projectId
          GROUP BY t.language_id
        )
        SELECT
          p.id,
          l.id,
          l.tag,
          l.name,
          l.original_name,
          l.flag_emoji,
          COALESCE(s.translated_keys, 0),
          COALESCE(s.translated_words, 0),
          COALESCE(s.reviewed_keys, 0),
          COALESCE(s.reviewed_words, 0)
        FROM project p
        JOIN language l ON p.id = l.project_id
        LEFT JOIN lang_stats s ON s.language_id = l.id
        WHERE p.id = :projectId
        """,
        ).setParameter("projectId", projectId)

    if (branchId != null) {
      query.setParameter("branchId", branchId)
    }

    return (query.resultList as List<Array<Any?>>).map { row ->
      ProjectLanguageStatsResultView(
        projectId = (row[0] as Number).toLong(),
        languageId = (row[1] as Number?)?.toLong(),
        languageTag = row[2] as String?,
        languageName = row[3] as String?,
        languageOriginalName = row[4] as String?,
        languageFlagEmoji = row[5] as String?,
        translatedKeys = (row[6] as Number).toLong(),
        translatedWords = (row[7] as Number).toLong(),
        reviewedKeys = (row[8] as Number).toLong(),
        reviewedWords = (row[9] as Number).toLong(),
      )
    }
  }
}
