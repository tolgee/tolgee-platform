package io.tolgee.component.cdn

import io.tolgee.component.cdn.cachePurging.CdnPurgingProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.cdn.Cdn
import io.tolgee.service.cdn.CdnService
import io.tolgee.service.export.ExportService
import org.springframework.stereotype.Component
import java.io.InputStream


@Component
class CdnUploader(
  private val cdnFileStorageProvider: CdnFileStorageProvider,
  private val exportService: ExportService,
  private val cdnService: CdnService,
  private val cdnPurgingProvider: CdnPurgingProvider,
  private val tolgeeProperties: TolgeeProperties
) {
  fun upload(cdnId: Long) {
    val cdn = cdnService.get(cdnId)

    val storage = getStorage(cdn)

    val files = exportService.export(cdn.project.id, cdn)
    val withFullPaths = files.mapKeys { "${cdn.slug}/${it.key}" }
    storeToStorage(withFullPaths, storage)
    purgeCacheIfConfigured(cdn, withFullPaths)
  }

  private fun purgeCacheIfConfigured(
    cdn: Cdn,
    withFullPaths: Map<String, InputStream>
  ) {
    val isDefaultStorage = cdn.cdnStorage == null
    if (isDefaultStorage) {
      cdnPurgingProvider.defaultPurging?.purgeForPaths(withFullPaths.keys)
    }
  }

  private fun storeToStorage(
    withFullPaths: Map<String, InputStream>,
    storage: FileStorage
  ) {
    withFullPaths.forEach {
      storage.storeFile(
        storageFilePath = it.key,
        bytes = it.value.readBytes()
      )
    }
  }

  private fun getStorage(cdn: Cdn) = cdn.cdnStorage
    ?.let {
      cdnFileStorageProvider.getStorage(
        config = it.storageConfig ?: throw IllegalStateException("No storage config stored")
      )
    }
    ?: cdnFileStorageProvider.getCdnStorageWithDefaultClient()
}
