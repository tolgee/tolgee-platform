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
    val builder = builderForConnectionString(config.connectionString)
    try {
      val containerClient =
        builder
          .buildClient()
          .getBlobContainerClient(config.containerName)
      return AzureBlobFileStorage(containerClient)
    } catch (e: Exception) {
      throw BadRequestException(Message.CANNOT_CREATE_AZURE_STORAGE_CLIENT, e)
    }
  }

  private fun builderForConnectionString(connectionString: String?): BlobServiceClientBuilder {
    try {
      return BlobServiceClientBuilder().connectionString(connectionString)
    } catch (e: IllegalArgumentException) {
      throw InvalidConnectionStringException()
    }
  }
}
