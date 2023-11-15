package io.tolgee.component.cdn

import io.tolgee.component.cdn.cachePurging.CdnPurgingProvider
import io.tolgee.model.cdn.Cdn
import io.tolgee.service.cdn.CdnService
import io.tolgee.service.export.ExportService
import org.springframework.stereotype.Component


@Component
class CdnUploader(
  private val cdnFileStorageProvider: CdnFileStorageProvider,
  private val exportService: ExportService,
  private val cdnService: CdnService,
  private val cdnPurgingProvider: CdnPurgingProvider
) {
  fun upload(cdnId: Long) {
    val cdn = cdnService.get(cdnId)

    val storage = getStorage(cdn)

    val files = exportService.export(cdn.project.id, cdn)
    val withFullPaths = files.mapKeys { "${cdn.slug}/${it.key}" }
    withFullPaths.forEach {
      storage.storeFile(
        storageFilePath = it.key,
        bytes = it.value.readBytes()
      )
    }
    cdnPurgingProvider.defaultPurging?.purgeForPaths(withFullPaths.keys)
  }

  private fun getStorage(cdn: Cdn) = cdn.cdnStorage
    ?.let {
      cdnFileStorageProvider.getStorage(
        config = it.storageConfig ?: throw IllegalStateException("No storage config stored")
      )
    }
    ?: cdnFileStorageProvider.getCdnStorageWithDefaultClient()
}
