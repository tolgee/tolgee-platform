package io.tolgee.component.contentDelivery

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.service.export.ExportService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class ContentDeliveryUploader(
  private val contentDeliveryFileStorageProvider: ContentDeliveryFileStorageProvider,
  private val exportService: ExportService,
  private val contentDeliveryConfigService: ContentDeliveryConfigService,
  private val contentDeliveryCachePurgingProvider: ContentDeliveryCachePurgingProvider,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  fun upload(contentDeliveryConfigId: Long) {
    val config = contentDeliveryConfigService.get(contentDeliveryConfigId)
    logger.debug("Uploading content delivery config ${config.id}")
    val storage = getStorage(config)
    val files = exportService.export(config.project.id, config)
    val withFullPaths = files.mapKeys { "${config.slug}/${it.key}" }
    pruneIfNeeded(config, storage)
    storeToStorage(withFullPaths, storage)
    purgeCacheIfConfigured(config, files.keys)
    config.lastPublished = currentDateProvider.date
    contentDeliveryConfigService.save(config)
  }

  private fun pruneIfNeeded(
    config: ContentDeliveryConfig,
    storage: FileStorage,
  ) {
    if (config.pruneBeforePublish) {
      storage.pruneDirectory(config.slug)
    }
  }

  private fun purgeCacheIfConfigured(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  ) {
    val isDefaultStorage = contentDeliveryConfig.contentStorage == null
    if (isDefaultStorage) {
      contentDeliveryCachePurgingProvider.purgings.forEach {
        it.purgeForPaths(contentDeliveryConfig, paths)
      }
    }
  }

  private fun storeToStorage(
    withFullPaths: Map<String, InputStream>,
    storage: FileStorage,
  ) {
    withFullPaths.forEach {
      storage.storeFile(
        storageFilePath = it.key,
        bytes = it.value.readBytes(),
      )
    }
  }

  private fun getStorage(contentDeliveryConfig: ContentDeliveryConfig) =
    contentDeliveryConfig.contentStorage
      ?.let {
        contentDeliveryFileStorageProvider.getStorage(
          config = it.storageConfig ?: throw IllegalStateException("No storage config stored"),
        )
      }
      ?: contentDeliveryFileStorageProvider.getContentStorageWithDefaultClient()
}
