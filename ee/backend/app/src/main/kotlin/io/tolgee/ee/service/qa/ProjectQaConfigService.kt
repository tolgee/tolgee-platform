package io.tolgee.ee.service.qa

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Language
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.repository.qa.LanguageQaConfigRepository
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectQaConfigService(
  private val projectQaConfigRepository: ProjectQaConfigRepository,
  private val languageQaConfigRepository: LanguageQaConfigRepository,
  private val projectService: ProjectService,
  private val entityManager: EntityManager,
  private val qaRecheckService: QaRecheckService,
  private val languageStatsService: LanguageStatsService,
  private val languageService: LanguageService,
) {
  @Transactional
  fun getOrCreateConfig(projectId: Long): ProjectQaConfig {
    return projectQaConfigRepository.findByProjectId(projectId)
      ?: ProjectQaConfig(
        project = projectService.get(projectId),
      ).also { projectQaConfigRepository.save(it) }
  }

  @Transactional
  fun setQaEnabled(
    projectId: Long,
    enabled: Boolean,
  ) {
    val project = projectService.get(projectId)
    if (project.useQaChecks == enabled) return
    project.useQaChecks = enabled
    projectService.save(project)

    if (enabled) {
      // Only recheck translations that changed (became stale) while QA was off.
      // The stale flag keeps working regardless of the QA-enabled toggle, so when
      // the user flips QA back on we don't need a full recheck — just the diff.
      qaRecheckService.recheckTranslations(projectId, onlyStale = true)
    } else {
      languageStatsService.refreshLanguageStats(projectId)
    }
  }

  fun getSettings(projectId: Long): Map<QaCheckType, QaCheckSeverity> {
    val config = projectQaConfigRepository.findByProjectId(projectId)
    val stored = config?.settings ?: emptyMap()
    return QaCheckType.entries.associateWith { type ->
      stored[type] ?: type.defaultSeverity
    }
  }

  fun getEnabledCheckTypesForProject(projectId: Long): Set<QaCheckType> {
    val settings = getSettings(projectId)
    return settings.filterValues { it != QaCheckSeverity.OFF }.keys
  }

  fun getEnabledCheckTypesForLanguage(
    projectId: Long,
    languageId: Long,
  ): Set<QaCheckType> {
    val settings = getResolvedSettingsForLanguage(projectId, languageId)
    return settings.filterValues { it != QaCheckSeverity.OFF }.keys
  }

  fun getSettingsForLanguage(
    projectId: Long,
    languageId: Long,
  ): Map<QaCheckType, QaCheckSeverity>? {
    val langConfig = languageQaConfigRepository.findByLanguageProjectIdAndLanguageId(projectId, languageId)
    return langConfig?.settings?.toMap()
  }

  fun getResolvedSettingsForLanguage(
    projectId: Long,
    languageId: Long,
  ): Map<QaCheckType, QaCheckSeverity> {
    val globalSettings = getSettings(projectId)
    val langConfig =
      languageQaConfigRepository.findByLanguageProjectIdAndLanguageId(projectId, languageId)
        ?: return globalSettings
    val languageSettings = langConfig.settings.toMutableMap()
    for (type in QaCheckType.entries) {
      languageSettings[type] = languageSettings[type] ?: globalSettings[type] ?: type.defaultSeverity
    }
    return languageSettings.toMap()
  }

  fun getSettingsForAllLanguages(projectId: Long): List<LanguageQaConfig> {
    return languageQaConfigRepository.findAllByLanguageProjectId(projectId)
  }

  // Config changes while QA is disabled would not trigger a recheck, so the
  // persisted QA issues would silently go out of sync with the new settings.
  private fun requireQaEnabled(projectId: Long) {
    val project = projectService.get(projectId)
    if (!project.useQaChecks) {
      throw BadRequestException(Message.QA_CHECKS_NOT_ENABLED)
    }
  }

  @Transactional
  fun updateSettings(
    projectId: Long,
    settings: Map<QaCheckType, QaCheckSeverity?>,
  ) {
    requireQaEnabled(projectId)
    val oldSettings = getSettings(projectId)
    val config = getOrCreateConfig(projectId)

    for ((type, severity) in settings) {
      if (severity == null) {
        config.settings.remove(type)
        continue
      }
      config.settings[type] = severity
    }
    projectQaConfigRepository.save(config)

    val changedTypes = settings.filter { (type, severity) -> oldSettings[type] != severity }.keys
    recheckIfEnabled(projectId, changedTypes.toList())
  }

  @Transactional
  fun updateLanguageSettings(
    projectId: Long,
    languageId: Long,
    settings: Map<QaCheckType, QaCheckSeverity?>,
  ) {
    requireQaEnabled(projectId)
    // Validate that the language belongs to this project
    languageService.getEntity(languageId, projectId)

    val langConfig =
      languageQaConfigRepository.findByLanguageProjectIdAndLanguageId(projectId, languageId)
        ?: LanguageQaConfig(
          language = entityManager.getReference(Language::class.java, languageId),
        )

    val oldSettings = langConfig.settings
    val changedTypes = settings.filter { (type, severity) -> oldSettings[type] != severity }.keys

    for ((type, severity) in settings) {
      if (severity == null) {
        langConfig.settings.remove(type)
        continue
      }
      langConfig.settings[type] = severity
    }
    languageQaConfigRepository.save(langConfig)

    recheckIfEnabled(projectId, changedTypes.toList(), listOf(languageId))
  }

  @Transactional
  fun deleteLanguageSettings(
    projectId: Long,
    languageId: Long,
  ) {
    requireQaEnabled(projectId)
    val langConfig = languageQaConfigRepository.findByLanguageProjectIdAndLanguageId(projectId, languageId) ?: return
    val changedTypes = langConfig.settings.keys

    languageQaConfigRepository.delete(langConfig)

    recheckIfEnabled(projectId, changedTypes.toList(), listOf(languageId))
  }

  private fun recheckIfEnabled(
    projectId: Long,
    checkTypes: List<QaCheckType>,
    languageIds: List<Long>? = null,
  ) {
    val project = projectService.get(projectId)
    if (project.useQaChecks) {
      qaRecheckService.recheckTranslations(projectId, checkTypes = checkTypes, languageIds = languageIds)
    }
  }
}
