package io.tolgee.service.project

import io.tolgee.dtos.query_results.ProjectStatistics
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import io.tolgee.model.views.projectStats.ProjectStatsView
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.service.query_builders.LanguageStatsProvider
import io.tolgee.service.query_builders.ProjectStatsProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager
import javax.persistence.criteria.Expression
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.SetJoin

@Transactional
@Service
class ProjectStatsService(
  private val entityManager: EntityManager,
  private val activityRevisionRepository: ActivityRevisionRepository
) {
  fun getLanguageStats(projectId: Long): List<ProjectLanguageStatsResultView> {
    return LanguageStatsProvider(entityManager, projectId).getResult()
  }

  fun getProjectStats(projectId: Long): ProjectStatsView {
    return ProjectStatsProvider(entityManager, projectId).getResult()
  }

  fun getProjectDailyActivity(projectId: Long): Map<LocalDate, Long> {
    return activityRevisionRepository.getProjectDailyActivity(projectId).map {
      val date = LocalDate.parse(it[1] as String)
      LocalDate.from(date) to it[0] as Long
    }.toMap()
  }

  fun getProjectsTotals(projectIds: Iterable<Long>): List<ProjectStatistics> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createTupleQuery()
    val root = query.from(Project::class.java)
    val languages = root.join(Project_.languages, JoinType.LEFT)
    val keys = root.join(Project_.keys, JoinType.LEFT)
    val stateJoins = mutableMapOf<TranslationState, SetJoin<Key, Translation>>()
    val stateSelects = linkedMapOf<TranslationState, Expression<Long>>()
    TranslationState.values().forEach { translationState ->
      val stateJoin = keys.join(Key_.translations, JoinType.LEFT)
      stateJoin.on(cb.equal(stateJoin.get(Translation_.state), translationState))
      stateJoins[translationState] = stateJoin
      stateSelects[translationState] = cb.countDistinct(stateJoin)
    }
    val keyCountSelect = cb.countDistinct(keys)
    val languageCountSelect = cb.countDistinct(languages)
    query.multiselect(
      root.get(Project_.id),
      keyCountSelect,
      languageCountSelect,
      *stateSelects.values.toTypedArray()
    )
    query.where(root.get(Project_.id).`in`(*projectIds.toList().toTypedArray()))
    query.groupBy(root.get(Project_.id))
    return entityManager.createQuery(query).resultList.map { tuple ->
      val stateMap = stateSelects.map { (state, select) ->
        state to tuple.get(select)
      }.toMap().toMutableMap()
      val untranslatedNotStored = tuple.get(languageCountSelect) * tuple.get(keyCountSelect) - stateMap.values.sum()
      stateMap[TranslationState.UNTRANSLATED] = (stateMap[TranslationState.UNTRANSLATED] ?: 0) + untranslatedNotStored
      ProjectStatistics(
        projectId = tuple.get(root.get(Project_.id)),
        languageCount = tuple.get(languageCountSelect),
        keyCount = tuple.get(keyCountSelect),
        translationStateCounts = stateMap,
      )
    }
  }
}
