package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectQaConfigService(
  private val projectQaConfigRepository: ProjectQaConfigRepository,
  private val projectService: ProjectService,
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

  fun getEnabledCheckTypes(projectId: Long): Set<QaCheckType> {
    val settings = getSettings(projectId)
    return settings.filterValues { it != QaCheckSeverity.OFF }.keys
  }

  @Transactional
  fun updateSettings(
    projectId: Long,
    settings: Map<QaCheckType, QaCheckSeverity>,
  ) {
    val config = getOrCreateConfig(projectId)
    config.settings = settings.toMutableMap()
    projectQaConfigRepository.save(config)
  }
}
