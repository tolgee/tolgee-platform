package io.tolgee.ee.service.qa

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.repository.qa.LanguageQaConfigRepository
import io.tolgee.repository.qa.ProjectQaConfigRepository
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
) {
  @Transactional
  fun getOrCreateConfig(projectId: Long): ProjectQaConfig {
    return projectQaConfigRepository.findByProjectId(projectId)
      ?: ProjectQaConfig(
        project = projectService.get(projectId),
      ).also { projectQaConfigRepository.save(it) }
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
    val langConfig =
      languageQaConfigRepository.findByProjectIdAndLanguageId(projectId, languageId)
    return langConfig?.settings?.toMap()
  }

  fun getResolvedSettingsForLanguage(
    projectId: Long,
    languageId: Long,
  ): Map<QaCheckType, QaCheckSeverity> {
    val globalSettings = getSettings(projectId)
    val langConfig =
      languageQaConfigRepository.findByProjectIdAndLanguageId(projectId, languageId)
        ?: return globalSettings
    val languageSettings = langConfig.settings.toMutableMap()
    for (type in QaCheckType.entries) {
      languageSettings[type] = languageSettings[type] ?: globalSettings[type] ?: type.defaultSeverity
    }
    return langConfig.settings.toMap()
  }

  fun getSettingsForAllLanguages(projectId: Long): List<LanguageQaConfig> {
    return languageQaConfigRepository.findAllByProjectId(projectId)
  }

  @Transactional
  fun updateSettings(
    projectId: Long,
    settings: Map<QaCheckType, QaCheckSeverity?>,
  ) {
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
    qaRecheckService.recheckTranslations(projectId, checkTypes = changedTypes.toList())
  }

  @Transactional
  fun updateLanguageSettings(
    projectId: Long,
    languageId: Long,
    settings: Map<QaCheckType, QaCheckSeverity?>,
  ) {
    val langConfig =
      languageQaConfigRepository.findByProjectIdAndLanguageId(projectId, languageId)
        ?: LanguageQaConfig(
          project = entityManager.getReference(Project::class.java, projectId),
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

    qaRecheckService.recheckTranslations(
      projectId,
      checkTypes = changedTypes.toList(),
      languageIds = listOf(languageId),
    )
  }

  @Transactional
  fun deleteLanguageSettings(
    projectId: Long,
    languageId: Long,
  ) {
    val langConfig = languageQaConfigRepository.findByProjectIdAndLanguageId(projectId, languageId) ?: return
    val changedTypes = langConfig.settings.keys

    languageQaConfigRepository.delete(langConfig)

    qaRecheckService.recheckTranslations(
      projectId,
      checkTypes = changedTypes.toList(),
      languageIds = listOf(languageId),
    )
  }
}
