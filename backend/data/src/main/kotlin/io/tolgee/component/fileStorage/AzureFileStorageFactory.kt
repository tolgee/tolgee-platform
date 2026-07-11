package io.tolgee.component.fileStorage

import com.azure.storage.blob.BlobServiceClientBuilder
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.InvalidConnectionStringException
import io.tolgee.model.contentDelivery.AzureBlobConfig
import org.springframework.stereotype.Component

@Component
class AzureFileStorageFactory {
  fun create(config: AzureBlobConfig): AzureBlobFileStorage {
    try {
      val connectionString = config.connectionString
      val blobServiceClient =
        BlobServiceClientBuilder()
          .connectionString(connectionString)
          .buildClient()
      val containerClient = blobServiceClient.getBlobContainerClient(config.containerName)
      return AzureBlobFileStorage(containerClient)
    } catch (e: Exception) {
      if (e is IllegalArgumentException && e.message == "Invalid connection string.") {
        throw InvalidConnectionStringException()
      }
      throw BadRequestException(Message.CANNOT_CREATE_AZURE_STORAGE_CLIENT)
    }
  }
}
