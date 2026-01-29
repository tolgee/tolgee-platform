package io.tolgee.service.project

import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.model.ILanguage
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.Branch_
import io.tolgee.model.key.Key_
import io.tolgee.model.views.projectStats.ProjectStatsView
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.service.queryBuilders.ProjectStatsProvider
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.JoinType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional
@Service
class ProjectStatsService(
  private val entityManager: EntityManager,
  private val activityRevisionRepository: ActivityRevisionRepository,
) {
  fun getProjectStats(
    projectId: Long,
    branchId: Long? = null,
  ): ProjectStatsView {
    return ProjectStatsProvider(entityManager, projectId, branchId).getResult()
  }

  fun getProjectDailyActivity(projectId: Long): Map<LocalDate, Long> {
    return activityRevisionRepository
      .getProjectDailyActivity(projectId)
      .map {
        val date = LocalDate.parse(it[1] as String)
        LocalDate.from(date) to it[0] as Long
      }.toMap()
  }

  fun getProjectsTotals(projectIds: Iterable<Long>): Map<Long, ProjectTotals> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createTupleQuery()
    val root = query.from(Project::class.java)
    val languages = root.join(Project_.languages, JoinType.LEFT)
    val keys = root.join(Project_.keys, JoinType.LEFT)
    val branch = keys.join(Key_.branch, JoinType.LEFT)
    val keyCountSelect = cb.countDistinct(keys)
    val languageCountSelect = cb.countDistinct(languages)
    query.multiselect(
      root.get(Project_.id),
      keyCountSelect,
      languageCountSelect,
    )
    query.where(
      cb.and(
        root.get(Project_.id).`in`(*projectIds.toList().toTypedArray()),
        cb.or(
          cb.isNull(keys.get(Key_.branch)),
          cb.isTrue(branch.get(Branch_.isDefault)),
        ),
      ),
    )
    query.groupBy(root.get(Project_.id))
    return entityManager.createQuery(query).resultList.associate { tuple ->
      tuple.get(root.get(Project_.id)) to ProjectTotals(tuple.get(languageCountSelect), tuple.get(keyCountSelect))
    }
  }

  fun computeProjectTotals(
    baseLanguage: ILanguage,
    languageStats: List<LanguageStatsDto>,
  ): ProjectStateTotals {
    val baseStats =
      languageStats.find { it.languageId == baseLanguage.id }
        ?: return ProjectStateTotals(0, 0.0, 0.0)

    val baseWordsCount = baseStats.translatedWords + baseStats.reviewedWords
    val nonBaseLanguages = languageStats.filterNot { it.languageId == baseLanguage.id }

    if (nonBaseLanguages.isEmpty()) {
      return ProjectStateTotals(
        baseWordsCount = baseWordsCount,
        translatedPercent = baseStats.translatedPercentage,
        reviewedPercent = baseStats.reviewedPercentage,
      )
    }

    val allNonBaseTotalBaseWords = baseWordsCount * nonBaseLanguages.size
    val allNonBaseTotalTranslatedWords = nonBaseLanguages.sumOf { it.translatedWords }
    val allNonBaseTotalReviewedWords = nonBaseLanguages.sumOf { it.reviewedWords }

    val translatedPercent = (allNonBaseTotalTranslatedWords.toDouble() / allNonBaseTotalBaseWords) * 100
    val reviewedPercent = (allNonBaseTotalReviewedWords.toDouble() / allNonBaseTotalBaseWords) * 100

    return ProjectStateTotals(
      baseWordsCount = baseWordsCount,
      translatedPercent = translatedPercent,
      reviewedPercent = reviewedPercent,
    )
  }

  data class ProjectStateTotals(
    val baseWordsCount: Long,
    val translatedPercent: Double,
    val reviewedPercent: Double,
  )

  data class ProjectTotals(
    val languageCount: Long,
    val keyCount: Long,
  )
}
