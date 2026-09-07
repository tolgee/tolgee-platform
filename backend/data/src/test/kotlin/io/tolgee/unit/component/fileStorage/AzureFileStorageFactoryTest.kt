package io.tolgee.unit.component.fileStorage

import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.InvalidConnectionStringException
import io.tolgee.model.contentDelivery.AzureBlobConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AzureFileStorageFactoryTest {
  companion object {
    const val BAD_ENDPOINT_CONNECTION_STRING =
      "DefaultEndpointsProtocol=http;AccountName=unit;AccountKey=dGVzdA==;BlobEndpoint=::not-a-url::;"
  }

  private val factory = AzureFileStorageFactory()

  @Test
  fun `keeps the SDK cause when the connection string is malformed`() {
    val exception =
      assertThrows<InvalidConnectionStringException> {
        factory.create(config(connectionString = "not-a-connection-string", containerName = "container"))
      }
    exception.cause.assert.isInstanceOf(IllegalArgumentException::class.java)
    exception.cause!!
      .message.assert
      .isEqualTo("Invalid connection string.")
  }

  @Test
  fun `keeps the cause when the client cannot be created for another reason`() {
    val exception =
      assertThrows<BadRequestException> {
        factory.create(config(connectionString = BAD_ENDPOINT_CONNECTION_STRING, containerName = "container"))
      }
    exception.cause.assert.isNotNull
  }

  private fun config(
    connectionString: String?,
    containerName: String?,
  ) = object : AzureBlobConfig {
    override var connectionString: String? = connectionString
    override var containerName: String? = containerName
  }
}
