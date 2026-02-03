package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.ImportSettings
import io.tolgee.model.dataImport.ImportSettingsId
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImportSettingsService(
  private val entityManager: EntityManager,
  private val importService: ImportService,
  private val projectService: ProjectService,
) {
  @Transactional
  fun store(
    userAccount: UserAccount,
    projectId: Long,
    settings: IImportSettings,
  ): ImportSettings {
    val existing = getOrCreateSettings(userAccount, projectId)
    val oldSettings = existing.clone()
    existing.assignFrom(settings)
    entityManager.persist(existing)
    importService.applySettings(userAccount, projectId, oldSettings, settings)
    return existing
  }

  @Transactional
  fun get(
    userAccount: UserAccount,
    projectId: Long,
  ): IImportSettings {
    val icuPlaceholders = projectService.get(projectId).icuPlaceholders
    return getOrCreateSettings(userAccount, projectId).also {
      if (!icuPlaceholders) {
        it.convertPlaceholdersToIcu = false
      }
    }
  }

  private fun getOrCreateSettings(
    userAccount: UserAccount,
    projectId: Long,
  ): ImportSettings {
    return (
      entityManager
        .find(ImportSettings::class.java, ImportSettingsId(userAccount.id, projectId))
        ?: ImportSettings(
          entityManager.getReference(Project::class.java, projectId),
        ).apply {
          this.userAccount = userAccount
        }
    )
  }

  @Transactional
  fun deleteAllByProject(projectId: Long) {
    entityManager
      .createQuery("delete from ImportSettings is where is.project.id = :projectId")
      .setParameter("projectId", projectId)
      .executeUpdate()
  }
}
