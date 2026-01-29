package io.tolgee.service.project

import io.tolgee.component.LockingProvider
import io.tolgee.dtos.queryResults.LanguageStatsDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.LanguageStats
import io.tolgee.model.branching.Branch
import io.tolgee.model.views.projectStats.ProjectLanguageStatsResultView
import io.tolgee.repository.LanguageStatsRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.queryBuilders.LanguageStatsProvider
import io.tolgee.util.Logging
import io.tolgee.util.debug
import io.tolgee.util.executeInNewRepeatableTransaction
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class LanguageStatsService(
  private val languageService: LanguageService,
  private val projectStatsService: ProjectStatsService,
  private val languageStatsRepository: LanguageStatsRepository,
  private val translationRepository: TranslationRepository,
  private val entityManager: EntityManager,
  private val projectService: ProjectService,
  private val lockingProvider: LockingProvider,
  private val platformTransactionManager: PlatformTransactionManager,
) : Logging {
  fun refreshLanguageStats(
    projectId: Long,
    branchId: Long? = null,
  ) {
    lockingProvider.withLocking("refresh-lang-stats-$projectId-${branchId ?: "default"}") {
      executeInNewRepeatableTransaction(platformTransactionManager) tx@{
        val languages = languageService.findAll(projectId)
        val allRawLanguageStats = getLanguageStatsRaw(projectId, branchId)
        try {
          val baseLanguage = projectService.getOrAssignBaseLanguage(projectId)
          val rawBaseLanguageStats =
            allRawLanguageStats.find { it.languageId == baseLanguage.id }
              ?: return@tx
          val projectStats = projectStatsService.getProjectStats(projectId, branchId)
          val languageStats =
            languageStatsRepository
              .getAllByProjectIdAndBranchId(
                projectId,
                branchId,
              ).associateBy { it.language.id }
              .toMutableMap()

          allRawLanguageStats
            .sortedBy { it.languageName }
            .sortedBy { it.languageId != rawBaseLanguageStats.languageId }
            .forEach { rawLanguageStats ->
              val baseWords = rawBaseLanguageStats.translatedWords + rawBaseLanguageStats.reviewedWords
              val translatedOrReviewedKeys = rawLanguageStats.translatedKeys + rawLanguageStats.reviewedKeys
              val translatedOrReviewedWords = rawLanguageStats.translatedWords + rawLanguageStats.reviewedWords
              val untranslatedWords = baseWords - translatedOrReviewedWords
              val language = languages.find { it.id == rawLanguageStats.languageId } ?: return@forEach
              val stats =
                languageStats.computeIfAbsent(language.id) {
                  LanguageStats(
                    entityManager.getReference(Language::class.java, language.id),
                    branchId?.let { entityManager.getReference(Branch::class.java, it) },
                  )
                }

              val lastUpdatedAt = translationRepository.getLastModifiedDate(language.id)

              stats.apply {
                translatedKeys = rawLanguageStats.translatedKeys
                translatedWords = rawLanguageStats.translatedWords
                translatedPercentage = rawLanguageStats.translatedWords.toDouble() / baseWords * 100
                reviewedKeys = rawLanguageStats.reviewedKeys
                reviewedWords = rawLanguageStats.reviewedWords
                reviewedPercentage = rawLanguageStats.reviewedWords.toDouble() / baseWords * 100
                untranslatedKeys = projectStats.keyCount - translatedOrReviewedKeys
                this.untranslatedWords = baseWords - translatedOrReviewedWords
                untranslatedPercentage = untranslatedWords.toDouble() / baseWords * 100
                translationsUpdatedAt = lastUpdatedAt
              }
            }

          languageStats.values.forEach {
            languageStatsRepository.save(it)
          }
          logger.debug {
            "Language stats refreshed for project $projectId: ${
              languageStats.values.joinToString("\n") {
                "Lang: ${it.language.tag} ${it.branch?.let {
                  "(branch: ${it.name}) "
                } ?: ""}- reviewed words: ${it.reviewedWords} translated words:${it.translatedWords}"
              }
            }"
          }
        } catch (e: NotFoundException) {
          logger.warn("Cannot save Language Stats due to NotFoundException. Project deleted too fast?", e)
        }
      }
    }
  }

  fun getLanguageStatsDtos(projectIds: List<Long>): Map<Long, List<LanguageStatsDto>> {
    val data =
      projectIds.associateWith { projectId ->
        languageStatsRepository.getDtosByProjectIdAndBranchId(projectId, null)
      }

    val emptyProjectsIds = projectIds.filter { data[it].isNullOrEmpty() }
    if (emptyProjectsIds.isEmpty()) {
      return data
    }

    emptyProjectsIds.forEach {
      refreshLanguageStats(it)
    }

    return projectIds.associateWith { projectId ->
      languageStatsRepository.getDtosByProjectIdAndBranchId(projectId, null)
    }
  }

  fun getLanguageStats(
    projectId: Long,
    branchId: Long? = null,
  ): List<LanguageStatsDto> {
    val data = languageStatsRepository.getDtosByProjectIdAndBranchId(projectId, branchId)
    if (data.isNotEmpty()) {
      return data
    }

    refreshLanguageStats(projectId, branchId)
    return languageStatsRepository.getDtosByProjectIdAndBranchId(projectId, branchId)
  }

  private fun getLanguageStatsRaw(
    projectId: Long,
    branchId: Long?,
  ): List<ProjectLanguageStatsResultView> {
    return LanguageStatsProvider(entityManager, projectId, branchId).getResultForSingleProject()
  }
}
