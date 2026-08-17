package io.tolgee.component.contentDelivery

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.resolveExportContentType
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.service.export.ExportService
import io.tolgee.util.Logging
import io.tolgee.util.formatPathsForLog
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    var files = exportService.export(config.project.id, config)

    if (config.zip) {
      files = createZipArchive(files)
    }

    pruneIfNeeded(config, storage)
    storeToStorage(config, files, storage)
    purgeCacheIfConfigured(config, files.keys)

    config.lastPublished = currentDateProvider.date
    config.lastPublishedFiles = files.keys.toList()
    contentDeliveryConfigService.save(config)
  }

  private fun getStorage(contentDeliveryConfig: ContentDeliveryConfig) =
    contentDeliveryConfig.contentStorage
      ?.let {
        contentDeliveryFileStorageProvider.getStorage(
          config = it.storageConfig ?: throw IllegalStateException("No storage config stored"),
        )
      }
      ?: contentDeliveryFileStorageProvider.getContentStorageWithDefaultClient()

  private fun createZipArchive(files: Map<String, InputStream>): Map<String, InputStream> {
    val zipFileName = "translations.zip"
    val outputStream = ByteArrayOutputStream()
    ZipOutputStream(outputStream).use { zip ->
      files.forEach { (path, input) ->
        zip.putNextEntry(ZipEntry(path))
        input.use { it.copyTo(zip) }
        zip.closeEntry()
      }
    }
    return mapOf(zipFileName to outputStream.toByteArray().inputStream())
  }

  private fun pruneIfNeeded(
    config: ContentDeliveryConfig,
    storage: FileStorage,
  ) {
    if (config.pruneBeforePublish) {
      try {
        storage.pruneDirectory(config.slug)
      } catch (e: Exception) {
        throw BadRequestException(
          Message.CONTENT_DELIVERY_PRUNE_FAILED,
          cause = e,
        )
      }
    }
  }

  private fun storeToStorage(
    config: ContentDeliveryConfig,
    files: Map<String, InputStream>,
    storage: FileStorage,
  ) {
    val pathsWithoutContentType = mutableListOf<String>()

    files.forEach { (path, stream) ->
      val contentType = resolveExportContentType(config.format, config.zip, path)
      if (contentType == null) {
        pathsWithoutContentType.add(path)
      }
      storage.storeFile(
        storageFilePath = "${config.slug}/$path",
        bytes = stream.readBytes(),
        contentType = contentType,
      )
    }

    logUnresolvedContentTypes(config, pathsWithoutContentType)
  }

  private fun logUnresolvedContentTypes(
    config: ContentDeliveryConfig,
    pathsWithoutContentType: List<String>,
  ) {
    if (pathsWithoutContentType.isEmpty()) {
      return
    }
    logger.warn(
      "Content delivery config {} ({}) publishes {} file(s) without a content type: {}",
      config.id,
      config.format,
      pathsWithoutContentType.size,
      formatPathsForLog(pathsWithoutContentType),
    )
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
}
