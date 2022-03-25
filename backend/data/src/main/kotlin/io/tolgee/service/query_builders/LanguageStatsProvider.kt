package io.tolgee.service.query_builders

import io.tolgee.model.Language_
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Expression
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.persistence.criteria.Selection
import javax.persistence.criteria.SetJoin

open class LanguageStatsProvider(
  val entityManager: EntityManager,
  private val projectId: Long
) {

  val cb: CriteriaBuilder = entityManager.criteriaBuilder
  val query: CriteriaQuery<ProjectLanguageStatsResultView> = cb.createQuery(ProjectLanguageStatsResultView::class.java)

  private var project: Root<Project> = query.from(Project::class.java)
  private val languageJoin = project.join(Project_.languages)

  fun getResult(): MutableList<ProjectLanguageStatsResultView> {
    initQuery()
    return entityManager.createQuery(query).resultList
  }

  private fun initQuery() {
    val counts = listOf(TranslationState.TRANSLATED, TranslationState.REVIEWED).map { state ->
      selectWordCount(state) to selectKeyCount(state)
    }

    val selection = mutableListOf<Selection<*>>(
      languageJoin.get(Language_.id),
      languageJoin.get(Language_.tag),
      languageJoin.get(Language_.name),
      languageJoin.get(Language_.originalName),
      languageJoin.get(Language_.flagEmoji)
    )

    counts.forEach { (wordCount, keyCount) ->
      selection.add(keyCount)
      selection.add(wordCount)
    }

    query.multiselect(selection)

    query.groupBy(languageJoin.get(Language_.id))

    query.where(cb.equal(project.get(Project_.id), projectId))
  }

  private fun selectKeyCount(state: TranslationState): Selection<Int> {
    val sub = query.subquery(Int::class.java)
    val project = sub.from(Project::class.java)
    val keyJoin = project.join(Project_.keys)
    val targetTranslationsJoin = joinTargetTranslations(keyJoin, state)
    val count = cb.count(targetTranslationsJoin.get(Translation_.id)) as Expression<Int>
    return sub.select(count)
  }

  private fun selectWordCount(state: TranslationState): Selection<Int> {
    val sub = query.subquery(Int::class.java)
    val project = sub.from(Project::class.java)
    val keyJoin = project.join(Project_.keys)

    joinTargetTranslations(keyJoin, state)

    val baseTranslationJoin = keyJoin.join(Key_.translations, JoinType.LEFT).also { translation ->
      translation.on(
        cb.equal(translation.get(Translation_.language), project.get(Project_.baseLanguage))
      )
    }
    return sub.select(cb.sum(baseTranslationJoin.get(Translation_.wordCount)))
  }

  private fun joinTargetTranslations(
    keyJoin: SetJoin<Project, Key>,
    state: TranslationState
  ): SetJoin<Key, Translation> {
    return keyJoin.join(Key_.translations).also { translation ->
      translation.on(
        cb.and(
          cb.equal(
            translation.get(
              Translation_.state
            ),
            state
          ),
          cb.equal(translation.get(Translation_.language), languageJoin)
        )
      )
    }
  }

  fun CriteriaBuilder.translationInState(translation: Path<Translation>, state: TranslationState): Predicate {
    val stateValueEquals = this.equal(
      translation.get(
        Translation_.state
      ),
      state
    )
    if (state == TranslationState.UNTRANSLATED) {
      return cb.or(stateValueEquals, cb.isNull(translation.get(Translation_.text)))
    }
    return stateValueEquals
  }
}
