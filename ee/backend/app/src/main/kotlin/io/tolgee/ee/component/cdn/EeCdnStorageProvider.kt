package io.tolgee.ee.component.cdn

import io.tolgee.component.CdnStorageProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.service.CdnStorageService
import io.tolgee.model.cdn.CdnStorage
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Component

@Component
class EeCdnStorageProvider(
  private val cdnStorageService: CdnStorageService,
  private val projectService: ProjectService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider
) : CdnStorageProvider {
  override fun getStorage(projectId: Long, cdnStorageId: Long): CdnStorage {
    projectService.findDto(projectId)?.let {
      enabledFeaturesProvider.checkFeatureEnabled(it.organizationOwnerId, Feature.PROJECT_LEVEL_CDN_STORAGES)
    }
    return cdnStorageService.get(projectId, cdnStorageId)
  }
}
