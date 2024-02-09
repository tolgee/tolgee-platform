package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.ImportSettings
import io.tolgee.model.dataImport.ImportSettingsId
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImportSettingsService(
  private val entityManager: EntityManager,
) {
  @Transactional
  fun store(
    userAccount: UserAccount,
    projectId: Long,
    settings: IImportSettings,
  ): ImportSettings {
    val existing = getOrCreateSettings(userAccount, projectId)
    existing.assignFrom(settings)
    entityManager.persist(existing)
    return existing
  }

  fun get(
    userAccount: UserAccount,
    projectId: Long,
  ): IImportSettings {
    return getOrCreateSettings(userAccount, projectId)
  }

  private fun getOrCreateSettings(
    userAccount: UserAccount,
    projectId: Long,
  ) = (
    entityManager
      .find(ImportSettings::class.java, ImportSettingsId(userAccount.id, projectId))
      ?: ImportSettings(
        entityManager.getReference(Project::class.java, projectId),
      ).apply {
        this.userAccount = userAccount
      }
  )
}
