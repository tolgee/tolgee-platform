package io.tolgee.component.cdn

import com.azure.storage.blob.BlobServiceClientBuilder
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.service.export.ExportService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class Cdn(
  private val tolgeeProperties: TolgeeProperties,
  private val exportService: ExportService
) {

  @EventListener
  @Async
  fun listen(event: OnProjectActivityStoredEvent) {
    upload(event.activityRevision.projectId)
  }

  fun upload(projectId: Long?) {
    projectId ?: return
    exportService.export(projectId, ExportParams()).forEach {
      client.storeFile(
        storageFilePath = "$projectId/${it.key}",
        bytes = it.value.readBytes()
      )
    }
  }

  val client by lazy {
    val azureProps = tolgeeProperties.cdn.azure
    val connectionString = azureProps.connectionString
    val blobServiceClient = BlobServiceClientBuilder()
      .connectionString(connectionString)
      .buildClient()
    val containerClient = blobServiceClient.getBlobContainerClient(azureProps.containerName)
    AzureBlobFileStorage(containerClient)
  }
}
