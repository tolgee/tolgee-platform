package io.tolgee.component.cdn

import io.tolgee.model.cdn.Cdn
import io.tolgee.service.cdn.CdnService
import io.tolgee.service.export.ExportService
import org.springframework.stereotype.Component

@Component
class CdnUploader(
  private val cdnFileStorageProvider: CdnFileStorageProvider,
  private val exportService: ExportService,
  private val cdnService: CdnService
) {
  fun upload(cdnId: Long) {
    val cdn = cdnService.get(cdnId)

    val storage = getStorage(cdn)

    val files = exportService.export(cdn.project.id, cdn)
    files.forEach {
      storage.storeFile(
        storageFilePath = "${cdn.slug}/${it.key}",
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
