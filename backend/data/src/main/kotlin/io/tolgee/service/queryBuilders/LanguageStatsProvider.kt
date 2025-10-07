package io.tolgee.service.queryBuilders

import io.tolgee.model.Language_
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.branching.Branch_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.ListJoin
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Selection
import jakarta.persistence.criteria.Subquery

open class LanguageStatsProvider(
  val entityManager: EntityManager,
  private val projectIds: List<Long>,
) {
  private val cb: CriteriaBuilder = entityManager.criteriaBuilder
  val query: CriteriaQuery<ProjectLanguageStatsResultView> = cb.createQuery(ProjectLanguageStatsResultView::class.java)

  private var project: Root<Project> = query.from(Project::class.java)
  private val languageJoin = project.join(Project_.languages)

  fun getResultForSingleProject(): MutableList<ProjectLanguageStatsResultView> {
    initQuery()
    return entityManager.createQuery(query).resultList
  }

  private fun initQuery() {
    val counts =
      listOf(TranslationState.TRANSLATED, TranslationState.REVIEWED).map { state ->
        selectWordCount(state) to selectKeyCount(state)
      }

    val projectId = project.get(Project_.id)
    val languageId = languageJoin.get(Language_.id)
    val selection =
      mutableListOf<Selection<*>>(
        projectId,
        languageId,
        languageJoin.get(Language_.tag),
        languageJoin.get(Language_.name),
        languageJoin.get(Language_.originalName),
        languageJoin.get(Language_.flagEmoji),
      )

    counts.forEach { (wordCount, keyCount) ->
      selection.add(keyCount)
      selection.add(wordCount)
    }

    query.multiselect(selection)

    query.groupBy(languageId, projectId)
    query.where(projectId.`in`(projectIds))
  }

  private fun selectKeyCount(state: TranslationState): Selection<Long> {
    val sub = query.subquery(Long::class.java)
    val subProject = sub.from(Project::class.java)
    val keyJoin = subProject.join(Project_.keys)
    joinDefaultBranch(sub, keyJoin)

    joinTargetTranslations(keyJoin, state)

    val countDistinctNames = cb.countDistinct(keyJoin.get(Key_.name))
    val coalesceCount = cb.coalesce<Long>().value(countDistinctNames).value(0L)

    return sub.select(coalesceCount)
  }

  private fun selectWordCount(state: TranslationState): Selection<Int> {
    val sub = query.subquery(Int::class.java)
    val project = sub.from(Project::class.java)
    val keyJoin = project.join(Project_.keys)
    joinDefaultBranch(sub, keyJoin)

    joinTargetTranslations(keyJoin, state)

    val baseTranslationJoin =
      keyJoin.join(Key_.translations, JoinType.LEFT).also { translation ->
        translation.on(
          cb.equal(translation.get(Translation_.language), project.get(Project_.baseLanguage)),
        )
      }

    val count = cb.sum(baseTranslationJoin.get(Translation_.wordCount))
    val coalesceCount = cb.coalesce<Int>()
    coalesceCount.value(count)
    coalesceCount.value(0)

    return sub.select(coalesceCount)
  }

  private fun joinDefaultBranch(
    subquery: Subquery<*>,
    join: ListJoin<Project, Key>,
  ) {
    val branchJoin = join.join(Key_.branch, JoinType.LEFT)
    subquery.where(
      cb.or(
        cb.isNull(join.get(Key_.branch)),
        cb.isTrue(branchJoin.get(Branch_.isDefault)),
      ),
    )
  }

  private fun joinTargetTranslations(
    keyJoin: ListJoin<Project, Key>,
    state: TranslationState,
  ): ListJoin<Key, Translation> {
    return keyJoin.join(Key_.translations).also { translation ->
      translation.on(
        cb.and(
          cb.equal(
            translation.get(
              Translation_.state,
            ),
            state,
          ),
          cb.equal(translation.get(Translation_.language), languageJoin),
        ),
      )
    }
  }
}
