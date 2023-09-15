package io.tolgee.component.cdn

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.service.CdnService
import io.tolgee.service.export.ExportService
import org.springframework.stereotype.Component

@Component
class CdnUploader(
  private val storageClientProvider: StorageClientProvider,
  private val exportService: ExportService,
  private val cdnService: CdnService
) {
  fun upload(cdnId: Long, exportParams: ExportParams) {
    val cdn = cdnService.get(cdnId)
    exportService.export(cdn.project.id, exportParams).forEach {
      val client = storageClientProvider()
      client.storeFile(
        storageFilePath = "${cdn.project.id}/${cdn.slug}/${it.key}",
        bytes = it.value.readBytes()
      )
    }
  }
}
