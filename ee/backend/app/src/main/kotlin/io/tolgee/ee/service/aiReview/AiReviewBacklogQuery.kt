package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.AiReviewKeyScopeSql.toKeyScope
import io.tolgee.ee.service.aiReview.matching.KeyScope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.service.aiReview.AiReviewEligibleText
import io.tolgee.service.aiReview.AiReviewTextHash
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.Tuple
import org.hibernate.jpa.HibernateHints
import org.springframework.stereotype.Component

@Component
class AiReviewBacklogQuery(
  private val entityManager: EntityManager,
) {
  data class CandidateGroup(
    val scope: KeyScope,
    val languageId: Long,
    val translationCount: Long,
    val hasScoreOnlyRow: Boolean,
  )

  data class Candidate(
    val translationId: Long,
    val scope: KeyScope,
    val languageId: Long,
    val hasScoreOnlyRow: Boolean,
  )

  fun groupCandidates(
    projectId: Long,
    baseLanguageId: Long,
    defaultBranchName: String,
  ): List<CandidateGroup> {
    val query =
      entityManager.createNativeQuery(
        """
        select $SCOPE_COLUMNS, t.language_id as language_id, (r.id is not null) as has_score_only_row,
               count(*) as translation_count
        $CANDIDATES
        group by 1, 2, 3, 4, 5
        """,
        Tuple::class.java,
      )
    return query
      .withParameters(projectId, baseLanguageId, defaultBranchName)
      .tuples()
      .map { row ->
        CandidateGroup(
          scope = row.toKeyScope(keyId = 0),
          languageId = (row.get("language_id") as Number).toLong(),
          translationCount = (row.get("translation_count") as Number).toLong(),
          hasScoreOnlyRow = row.get("has_score_only_row") as Boolean,
        )
      }
  }

  fun forEachCandidate(
    projectId: Long,
    baseLanguageId: Long,
    defaultBranchName: String,
    fn: (Candidate) -> Unit,
  ) {
    val query =
      entityManager.createNativeQuery(
        """
        select $SCOPE_COLUMNS, t.language_id as language_id, t.id as translation_id, k.id as key_id,
               (r.id is not null) as has_score_only_row
        $CANDIDATES
        order by t.id
        """,
        Tuple::class.java,
      )
    query
      .withParameters(projectId, baseLanguageId, defaultBranchName)
      .setHint(HibernateHints.HINT_FETCH_SIZE, STREAM_FETCH_SIZE)
      .resultStream
      .use { rows ->
        rows.forEach { result ->
          val row = result as Tuple
          fn(
            Candidate(
              translationId = (row.get("translation_id") as Number).toLong(),
              scope = row.toKeyScope(keyId = (row.get("key_id") as Number).toLong()),
              languageId = (row.get("language_id") as Number).toLong(),
              hasScoreOnlyRow = row.get("has_score_only_row") as Boolean,
            ),
          )
        }
      }
  }

  private fun Query.withParameters(
    projectId: Long,
    baseLanguageId: Long,
    defaultBranchName: String,
  ): Query =
    setParameter("projectId", projectId)
      .setParameter("baseLanguageId", baseLanguageId)
      .setParameter("defaultBranch", defaultBranchName)
      .setParameter("disabled", TranslationState.DISABLED.ordinal)
      .setParameter("reviewed", TranslationState.REVIEWED.ordinal)

  @Suppress("UNCHECKED_CAST")
  private fun Query.tuples(): List<Tuple> = resultList as List<Tuple>

  companion object {
    private const val STREAM_FETCH_SIZE = 1_000

    private const val SCOPE_COLUMNS = """
      coalesce(n.name, '') as namespace_name,
      coalesce(b.name, :defaultBranch) as branch_name,
      coalesce(kt.tag_names, '') as tag_names
    """

    // Human-reviewed translations are excluded by the state check: an AI approval always has a row.
    private val CANDIDATES = """
      from translation t
      join key k on k.id = t.key_id
      left join namespace n on n.id = k.namespace_id
      left join branch b on b.id = k.branch_id
      left join (
        select km.key_id, string_agg(tg.name, chr(31) order by tg.name) as tag_names
        from key_meta km
        join key_meta_tags kmt on kmt.key_metas_id = km.id
        join tag tg on tg.id = kmt.tags_id
        where tg.project_id = :projectId
        group by km.key_id
      ) kt on kt.key_id = k.id
      left join translation_ai_review r on r.translation_id = t.id
        and r.text_hash = ${AiReviewTextHash.CURRENT_TEXT_SQL}
      where k.project_id = :projectId
        and k.deleted_at is null
        and (b.id is null or b.deleted_at is null)
        and t.language_id <> :baseLanguageId
        and ${AiReviewEligibleText.SQL_NOT_REVIEWED}
        and (
          r.id is null
          or r.status in ('${AiReviewStatus.SCORE_ONLY_PASS}', '${AiReviewStatus.SCORE_ONLY_FAIL}')
        )
    """
  }
}
