package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.AiReviewKeyScopeSql.toKeyScope
import io.tolgee.ee.service.aiReview.matching.KeyScope
import io.tolgee.model.enums.TranslationState
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.Tuple
import org.springframework.stereotype.Component

@Component
class AiReviewBacklogQuery(
  private val entityManager: EntityManager,
) {
  data class CandidateGroup(
    val scope: KeyScope,
    val languageId: Long,
    val translationCount: Long,
  )

  data class Candidate(
    val translationId: Long,
    val scope: KeyScope,
    val languageId: Long,
  )

  fun groupCandidates(
    projectId: Long,
    baseLanguageId: Long,
    defaultBranchName: String,
  ): List<CandidateGroup> {
    val query =
      entityManager.createNativeQuery(
        """
        select ${AiReviewKeyScopeSql.COLUMNS}, t.language_id as language_id, count(*) as translation_count
        $CANDIDATES
        group by 1, 2, 3, t.language_id
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
        select ${AiReviewKeyScopeSql.COLUMNS}, t.language_id as language_id, t.id as translation_id, k.id as key_id
        $CANDIDATES
        order by t.id
        """,
        Tuple::class.java,
      )
    query
      .withParameters(projectId, baseLanguageId, defaultBranchName)
      .tuples()
      .forEach { row ->
        fn(
          Candidate(
            translationId = (row.get("translation_id") as Number).toLong(),
            scope = row.toKeyScope(keyId = (row.get("key_id") as Number).toLong()),
            languageId = (row.get("language_id") as Number).toLong(),
          ),
        )
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
    // Human-reviewed translations are excluded by the state check: an AI approval always has a row.
    private const val CANDIDATES = """
      from translation t
      join key k on k.id = t.key_id
      left join namespace n on n.id = k.namespace_id
      left join branch b on b.id = k.branch_id
      left join translation_ai_review r on r.translation_id = t.id
        and r.text_hash = encode(sha256(convert_to(coalesce(t.text, ''), 'UTF8')), 'hex')
      where k.project_id = :projectId
        and k.deleted_at is null
        and (b.id is null or b.deleted_at is null)
        and t.language_id <> :baseLanguageId
        and t.text ~ '\S'
        and t.state <> :disabled
        and t.state <> :reviewed
        and t.outdated = false
        and r.id is null
    """
  }
}
