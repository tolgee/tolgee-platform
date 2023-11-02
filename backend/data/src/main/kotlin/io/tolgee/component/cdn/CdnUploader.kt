package io.tolgee.component.cdn

import io.tolgee.model.cdn.CdnExporter
import io.tolgee.service.cdn.CdnExporterService
import io.tolgee.service.export.ExportService
import org.springframework.stereotype.Component

@Component
class CdnUploader(
  private val cdnFileStorageProvider: CdnFileStorageProvider,
  private val exportService: ExportService,
  private val cdnExporterService: CdnExporterService
) {
  fun upload(cdnExporterId: Long) {
    val cdnExporter = cdnExporterService.get(cdnExporterId)

    val storage = getStorage(cdnExporter)

    exportService.export(cdnExporter.project.id, cdnExporter).forEach {
      storage.storeFile(
        storageFilePath = "${cdnExporter.slug}/${it.key}",
        bytes = it.value.readBytes()
      )
    }
  }

  private fun getStorage(cdnExporter: CdnExporter) = cdnExporter.cdnStorage
    ?.let {
      cdnFileStorageProvider.getStorage(
        config = it.storageConfig ?: throw IllegalStateException("No storage config stored")
      )
    }
    ?: cdnFileStorageProvider.getCdnStorageWithDefaultClient()
}
