package io.tolgee.component.fileStorage

import com.azure.storage.blob.BlobServiceClientBuilder
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.InvalidConnectionStringException
import io.tolgee.model.contentDelivery.AzureBlobConfig
import org.springframework.stereotype.Component

@Component
class AzureFileStorageFactory {
  companion object {
    private const val SDK_INVALID_CONNECTION_STRING_MESSAGE = "Invalid connection string."
  }

  fun create(config: AzureBlobConfig): AzureBlobFileStorage {
    try {
      val blobServiceClient =
        BlobServiceClientBuilder()
          .connectionString(config.connectionString)
          .buildClient()
      val containerClient = blobServiceClient.getBlobContainerClient(config.containerName)
      return AzureBlobFileStorage(containerClient)
    } catch (e: Exception) {
      if (e is IllegalArgumentException && e.message == SDK_INVALID_CONNECTION_STRING_MESSAGE) {
        throw InvalidConnectionStringException(e)
      }
      throw BadRequestException(Message.CANNOT_CREATE_AZURE_STORAGE_CLIENT, e)
    }
  }
}
