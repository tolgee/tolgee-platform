package io.tolgee.unit.component.fileStorage

import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.exceptions.InvalidConnectionStringException
import io.tolgee.model.contentDelivery.AzureBlobConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AzureFileStorageFactoryTest {
  companion object {
    const val BAD_ENDPOINT_CONNECTION_STRING =
      "DefaultEndpointsProtocol=http;AccountName=unit;AccountKey=dGVzdA==;BlobEndpoint=::not-a-url::;"
    const val AZURITE_CONNECTION_STRING =
      "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
        "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
        "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
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
  fun `reports a missing connection string as invalid rather than as a client creation failure`() {
    assertThrows<InvalidConnectionStringException> {
      factory.create(config(connectionString = null, containerName = "container"))
    }
  }

  @Test
  fun `reports an unparsable endpoint as an invalid connection string, keeping the cause`() {
    val exception =
      assertThrows<InvalidConnectionStringException> {
        factory.create(config(connectionString = BAD_ENDPOINT_CONNECTION_STRING, containerName = "container"))
      }
    exception.cause.assert.isNotNull
  }

  @Test
  fun `accepts a well formed connection string`() {
    assertDoesNotThrow {
      factory.create(config(connectionString = AZURITE_CONNECTION_STRING, containerName = "container"))
    }
  }

  private fun config(
    connectionString: String?,
    containerName: String?,
  ) = object : AzureBlobConfig {
    override var connectionString: String? = connectionString
    override var containerName: String? = containerName
  }
}
