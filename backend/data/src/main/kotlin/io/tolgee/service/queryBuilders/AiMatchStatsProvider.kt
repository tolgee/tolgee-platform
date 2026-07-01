package io.tolgee.service.queryBuilders

import io.tolgee.constants.MtServiceType
import io.tolgee.service.aiMatchStats.AiMatchActivityRow
import io.tolgee.service.aiMatchStats.CurrentTranslationRow
import io.tolgee.service.aiMatchStats.NotReviewedAgg
import io.tolgee.service.aiMatchStats.NotReviewedPromptAgg
import io.tolgee.service.aiMatchStats.PromptAgg
import io.tolgee.service.aiMatchStats.ReviewedLangAgg
import io.tolgee.service.aiMatchStats.TranslationAiMatchRow
import jakarta.persistence.EntityManager
import java.util.Date

/**
 * Read-only native SQL for AI-match stats.
 *
 * Note on jsonb: the Postgres `?` key-existence operator collides with the JDBC `?` parameter
 * marker, so `jsonb_exists(modifications, 'key')` is used instead.
 */
class AiMatchStatsProvider(
  private val entityManager: EntityManager,
) {
  companion object {
    /** 9 params/row; kept well under Postgres' 65535 bind-parameter ceiling. */
    private const val UPSERT_CHUNK = 1000
    private const val DELETE_CHUNK = 5000
  }

  fun getMaxRevisionId(projectId: Long): Long? {
    val result =
      entityManager
        .createNativeQuery("SELECT max(ar.id) FROM activity_revision ar WHERE ar.project_id = :projectId")
        .setParameter("projectId", projectId)
        .resultList
        .firstOrNull()
    return (result as Number?)?.toLong()
  }

  /**
   * The next slice of translations touched after [afterRevisionId], ordered by revision so the
   * backfill is resumable: at most [revisionLimit] revisions are scanned, and the returned
   * [AffectedSlice.lastRevisionId] is the cursor to resume from (null = caught up). A translation
   * may reappear across slices if it has revisions on both sides of the cut — harmless, since
   * recompute always walks its full history.
   */
  fun getAffectedSince(
    projectId: Long,
    afterRevisionId: Long,
    revisionLimit: Int,
  ): AffectedSlice {
    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT ame.entity_id, ar.id
          FROM activity_modified_entity ame
          JOIN activity_revision ar ON ar.id = ame.activity_revision_id
          WHERE ame.entity_class = 'Translation'
            AND ar.project_id = :projectId
            AND ar.id > :afterRevisionId
          ORDER BY ar.id
          LIMIT :revisionLimit
          """,
        ).setParameter("projectId", projectId)
        .setParameter("afterRevisionId", afterRevisionId)
        .setParameter("revisionLimit", revisionLimit)
        .resultList as List<Array<Any?>>
    if (rows.isEmpty()) return AffectedSlice(emptyList(), null)
    val ids = rows.map { (it[0] as Number).toLong() }.distinct()
    val lastRevisionId = (rows.last()[1] as Number).toLong()
    return AffectedSlice(ids, lastRevisionId)
  }

  data class AffectedSlice(
    val translationIds: List<Long>,
    val lastRevisionId: Long?,
  )

  /** prompt id -> ascending list of output-affecting edit timestamps (version boundaries). */
  fun getPromptVersionBoundaries(projectId: Long): Map<Long, List<Date>> {
    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT ame.entity_id, ar.timestamp
          FROM activity_modified_entity ame
          JOIN activity_revision ar ON ar.id = ame.activity_revision_id
          WHERE ame.entity_class = 'Prompt'
            AND ar.project_id = :projectId
            AND (
              jsonb_exists(ame.modifications, 'template')
              OR jsonb_exists(ame.modifications, 'providerName')
              OR jsonb_exists(ame.modifications, 'basicPromptOptions')
            )
          ORDER BY ame.entity_id, ar.timestamp
          """,
        ).setParameter("projectId", projectId)
        .resultList as List<Array<Any?>>
    return rows
      .groupBy({ (it[0] as Number).toLong() }, { it[1] as Date })
      .mapValues { (_, dates) -> dates.sorted() }
  }

  /** Current live state of each translation, keyed by id. Absent id = deleted or off the default branch. */
  fun getCurrentTranslations(
    projectId: Long,
    ids: Collection<Long>,
  ): Map<Long, CurrentTranslationRow> {
    if (ids.isEmpty()) return emptyMap()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT t.id, t.text, t.word_count, t.state, t.language_id
          FROM translation t
          JOIN key k ON k.id = t.key_id
          LEFT JOIN branch b ON b.id = k.branch_id
          WHERE t.id IN (:ids)
            AND k.project_id = :projectId
            AND k.deleted_at IS NULL
            AND (k.branch_id IS NULL OR b.is_default = true)
          """,
        ).setParameter("ids", ids)
        .setParameter("projectId", projectId)
        .resultList as List<Array<Any?>>
    return rows.associate { r ->
      (r[0] as Number).toLong() to
        CurrentTranslationRow(
          text = r[1] as String?,
          wordCount = (r[2] as Number?)?.toInt() ?: 0,
          state = (r[3] as Number).toInt(),
          languageId = (r[4] as Number).toLong(),
        )
    }
  }

  /** All Translation activity rows for the given ids, grouped by translation id, oldest first. */
  fun getActivityRowsForTranslations(
    projectId: Long,
    ids: Collection<Long>,
  ): Map<Long, List<AiMatchActivityRow>> {
    if (ids.isEmpty()) return emptyMap()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT ame.entity_id,
                 ar.timestamp,
                 jsonb_exists(ame.modifications, 'text')      AS has_text,
                 ame.modifications -> 'text'       ->> 'new'  AS text_new,
                 ame.modifications -> 'state'      ->> 'new'  AS state_new,
                 ame.modifications -> 'auto'       ->> 'new'  AS auto_new,
                 ame.modifications -> 'mtProvider' ->> 'new'  AS mt_new,
                 ame.modifications -> 'promptId'   ->> 'new'  AS prompt_new
          FROM activity_modified_entity ame
          JOIN activity_revision ar ON ar.id = ame.activity_revision_id
          WHERE ame.entity_class = 'Translation'
            AND ame.entity_id IN (:ids)
            AND ar.project_id = :projectId
          ORDER BY ame.entity_id, ar.timestamp, ar.id
          """,
        ).setParameter("ids", ids)
        .setParameter("projectId", projectId)
        .resultList as List<Array<Any?>>
    val grouped = LinkedHashMap<Long, MutableList<AiMatchActivityRow>>()
    rows.forEach { r ->
      val id = (r[0] as Number).toLong()
      grouped.getOrPut(id) { mutableListOf() }.add(
        AiMatchActivityRow(
          timestamp = r[1] as Date,
          textModified = r[2] as Boolean,
          textNew = r[3] as String?,
          stateNew = r[4] as String?,
          autoNew = (r[5] as String?)?.toBooleanStrictOrNull(),
          mtProviderNew = r[6] as String?,
          promptIdNew = (r[7] as String?)?.toLongOrNull(),
        ),
      )
    }
    return grouped
  }

  /** Bulk insert-or-update of materialized rows: one statement per [UPSERT_CHUNK] rows, no per-row round-trips. */
  fun upsertMatches(rows: List<TranslationAiMatchRow>) {
    rows.chunked(UPSERT_CHUNK).forEach { chunk ->
      val values =
        chunk.indices.joinToString(",") { i ->
          "(:t$i,:p$i,:l$i,:s$i,:r$i,:w$i,cast(:pi$i as bigint),cast(:mp$i as varchar),cast(:pv$i as timestamp))"
        }
      val query =
        entityManager.createNativeQuery(
          """
          INSERT INTO translation_ai_match
            (translation_id, project_id, language_id, match_score, reviewed_at, word_count, prompt_id, mt_provider, prompt_version_ts)
          VALUES $values
          ON CONFLICT (translation_id) DO UPDATE SET
            project_id = EXCLUDED.project_id,
            language_id = EXCLUDED.language_id,
            match_score = EXCLUDED.match_score,
            reviewed_at = EXCLUDED.reviewed_at,
            word_count = EXCLUDED.word_count,
            prompt_id = EXCLUDED.prompt_id,
            mt_provider = EXCLUDED.mt_provider,
            prompt_version_ts = EXCLUDED.prompt_version_ts
          """,
        )
      chunk.forEachIndexed { i, row ->
        query.setParameter("t$i", row.translationId)
        query.setParameter("p$i", row.projectId)
        query.setParameter("l$i", row.languageId)
        query.setParameter("s$i", row.matchScore)
        query.setParameter("r$i", row.reviewedAt)
        query.setParameter("w$i", row.wordCount)
        query.setParameter("pi$i", row.promptId)
        query.setParameter("mp$i", row.mtProvider)
        query.setParameter("pv$i", row.promptVersionTs)
      }
      query.executeUpdate()
    }
  }

  /** Bulk delete of materialized rows for translations that no longer qualify. No-op for absent ids. */
  fun deleteMatches(ids: List<Long>) {
    ids.chunked(DELETE_CHUNK).forEach { chunk ->
      entityManager
        .createNativeQuery("DELETE FROM translation_ai_match WHERE translation_id IN (:ids)")
        .setParameter("ids", chunk)
        .executeUpdate()
    }
  }

  fun getReviewedPerLanguage(
    projectId: Long,
    languageIds: Collection<Long>,
    after: Date,
    before: Date,
  ): List<ReviewedLangAgg> {
    if (languageIds.isEmpty()) return emptyList()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT language_id,
                 COUNT(*),
                 COALESCE(SUM(word_count), 0),
                 COALESCE(SUM(match_score::bigint * word_count), 0),
                 COALESCE(SUM(CASE WHEN match_score = 100 THEN word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN match_score >= 90 AND match_score < 100 THEN word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN match_score >= 80 AND match_score < 90 THEN word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN match_score >= 70 AND match_score < 80 THEN word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN match_score < 70 THEN word_count ELSE 0 END), 0),
                 COUNT(*) FILTER (WHERE match_score = 100),
                 COUNT(*) FILTER (WHERE match_score >= 90 AND match_score < 100),
                 COUNT(*) FILTER (WHERE match_score >= 80 AND match_score < 90),
                 COUNT(*) FILTER (WHERE match_score >= 70 AND match_score < 80),
                 COUNT(*) FILTER (WHERE match_score < 70)
          FROM translation_ai_match
          WHERE project_id = :projectId
            AND reviewed_at >= :after AND reviewed_at <= :before
            AND language_id IN (:languageIds)
          GROUP BY language_id
          """,
        ).setParameter("projectId", projectId)
        .setParameter("after", after)
        .setParameter("before", before)
        .setParameter("languageIds", languageIds)
        .resultList as List<Array<Any?>>
    return rows.map { r ->
      ReviewedLangAgg(
        languageId = (r[0] as Number).toLong(),
        reviewedKeys = (r[1] as Number).toLong(),
        reviewedWords = (r[2] as Number).toLong(),
        weightedScoreSum = (r[3] as Number).toLong(),
        b100Words = (r[4] as Number).toLong(),
        b9990Words = (r[5] as Number).toLong(),
        b8980Words = (r[6] as Number).toLong(),
        b7970Words = (r[7] as Number).toLong(),
        bnoWords = (r[8] as Number).toLong(),
        b100Keys = (r[9] as Number).toLong(),
        b9990Keys = (r[10] as Number).toLong(),
        b8980Keys = (r[11] as Number).toLong(),
        b7970Keys = (r[12] as Number).toLong(),
        bnoKeys = (r[13] as Number).toLong(),
      )
    }
  }

  fun getNotReviewedPerLanguage(
    projectId: Long,
    languageIds: Collection<Long>,
  ): List<NotReviewedAgg> {
    if (languageIds.isEmpty()) return emptyList()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT t.language_id, COUNT(*), COALESCE(SUM(t.word_count), 0)
          FROM translation t
          JOIN key k ON k.id = t.key_id
          LEFT JOIN branch b ON b.id = k.branch_id
          WHERE k.project_id = :projectId
            AND k.deleted_at IS NULL
            AND t.auto = true
            AND t.state NOT IN (2, 3)
            AND t.language_id IN (:languageIds)
            AND (k.branch_id IS NULL OR b.is_default = true)
          GROUP BY t.language_id
          """,
        ).setParameter("projectId", projectId)
        .setParameter("languageIds", languageIds)
        .resultList as List<Array<Any?>>
    return rows.map { r ->
      NotReviewedAgg(
        languageId = (r[0] as Number).toLong(),
        keys = (r[1] as Number).toLong(),
        words = (r[2] as Number).toLong(),
      )
    }
  }

  /**
   * AI cells still awaiting review, grouped by the prompt/engine that produced them. Read from the
   * live `translation` row (its `prompt_id`/`mt_provider` survive because an unreviewed cell hasn't
   * been edited). `mt_provider` is stored as the enum ordinal here, so it is mapped back to the name
   * to match the reviewed rows (which take it from the activity log, where it is the name).
   */
  fun getNotReviewedPerPrompt(
    projectId: Long,
    languageIds: Collection<Long>,
  ): List<NotReviewedPromptAgg> {
    if (languageIds.isEmpty()) return emptyList()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT t.prompt_id, t.mt_provider, p.name, COUNT(*), COALESCE(SUM(t.word_count), 0)
          FROM translation t
          JOIN key k ON k.id = t.key_id
          LEFT JOIN branch b ON b.id = k.branch_id
          LEFT JOIN prompt p ON p.id = t.prompt_id
          WHERE k.project_id = :projectId
            AND k.deleted_at IS NULL
            AND t.auto = true
            AND t.state NOT IN (2, 3)
            AND t.language_id IN (:languageIds)
            AND (k.branch_id IS NULL OR b.is_default = true)
          GROUP BY t.prompt_id, t.mt_provider, p.name
          """,
        ).setParameter("projectId", projectId)
        .setParameter("languageIds", languageIds)
        .resultList as List<Array<Any?>>
    return rows.map { r ->
      NotReviewedPromptAgg(
        promptId = (r[0] as Number?)?.toLong(),
        mtProvider = (r[1] as Number?)?.let { MtServiceType.entries.getOrNull(it.toInt())?.name },
        promptName = r[2] as String?,
        keys = (r[3] as Number).toLong(),
        words = (r[4] as Number).toLong(),
      )
    }
  }

  fun getPerPrompt(
    projectId: Long,
    languageIds: Collection<Long>,
    after: Date,
    before: Date,
  ): List<PromptAgg> {
    if (languageIds.isEmpty()) return emptyList()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(
          """
          SELECT tam.prompt_id, tam.mt_provider, tam.prompt_version_ts, p.name,
                 COUNT(*),
                 COALESCE(SUM(tam.word_count), 0),
                 COALESCE(SUM(tam.match_score::bigint * tam.word_count), 0),
                 COALESCE(SUM(CASE WHEN tam.match_score = 100 THEN tam.word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN tam.match_score >= 90 AND tam.match_score < 100 THEN tam.word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN tam.match_score >= 80 AND tam.match_score < 90 THEN tam.word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN tam.match_score >= 70 AND tam.match_score < 80 THEN tam.word_count ELSE 0 END), 0),
                 COALESCE(SUM(CASE WHEN tam.match_score < 70 THEN tam.word_count ELSE 0 END), 0)
          FROM translation_ai_match tam
          LEFT JOIN prompt p ON p.id = tam.prompt_id
          WHERE tam.project_id = :projectId
            AND tam.reviewed_at >= :after AND tam.reviewed_at <= :before
            AND tam.language_id IN (:languageIds)
          GROUP BY tam.prompt_id, tam.mt_provider, tam.prompt_version_ts, p.name
          """,
        ).setParameter("projectId", projectId)
        .setParameter("after", after)
        .setParameter("before", before)
        .setParameter("languageIds", languageIds)
        .resultList as List<Array<Any?>>
    return rows.map { r ->
      PromptAgg(
        promptId = (r[0] as Number?)?.toLong(),
        mtProvider = r[1] as String?,
        promptVersionTs = r[2] as Date?,
        promptName = r[3] as String?,
        reviewedKeys = (r[4] as Number).toLong(),
        reviewedWords = (r[5] as Number).toLong(),
        weightedScoreSum = (r[6] as Number).toLong(),
        b100Words = (r[7] as Number).toLong(),
        b9990Words = (r[8] as Number).toLong(),
        b8980Words = (r[9] as Number).toLong(),
        b7970Words = (r[10] as Number).toLong(),
        bnoWords = (r[11] as Number).toLong(),
      )
    }
  }
}
