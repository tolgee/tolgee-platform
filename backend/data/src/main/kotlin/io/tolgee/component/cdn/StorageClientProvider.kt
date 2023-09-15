package io.tolgee.component.cdn

import com.azure.storage.blob.BlobServiceClientBuilder
import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component

@Component
class StorageClientProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  operator fun invoke(): FileStorage {
    return client
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
