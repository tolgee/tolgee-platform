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
    val clientBuilder = buildWithConnectionString(config.connectionString)
    try {
      val containerClient = clientBuilder.buildClient().getBlobContainerClient(config.containerName)
      return AzureBlobFileStorage(containerClient)
    } catch (e: Exception) {
      throw BadRequestException(Message.CANNOT_CREATE_AZURE_STORAGE_CLIENT)
    }
  }

  private fun buildWithConnectionString(connectionString: String?): BlobServiceClientBuilder {
    try {
      return BlobServiceClientBuilder().connectionString(connectionString)
    } catch (e: IllegalArgumentException) {
      throw InvalidConnectionStringException()
    }
  }
}
