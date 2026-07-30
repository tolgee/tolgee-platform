package io.tolgee.unit.component.fileStorage

import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.dtos.contentDelivery.AzureContentStorageConfigDto
import io.tolgee.exceptions.InvalidConnectionStringException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AzureFileStorageFactoryTest {
  @Test
  fun `reports a malformed connection string as such`() {
    assertThatThrownBy { create(connectionString = "this is not a connection string") }
      .isInstanceOf(InvalidConnectionStringException::class.java)
  }

  @Test
  fun `reports a missing connection string as invalid rather than as a client creation failure`() {
    assertThatThrownBy { create(connectionString = null) }
      .isInstanceOf(InvalidConnectionStringException::class.java)
  }

  @Test
  fun `accepts a well formed connection string`() {
    assertThatCode { create(connectionString = AZURITE_CONNECTION_STRING) }.doesNotThrowAnyException()
  }

  private fun create(connectionString: String?) {
    val config =
      AzureContentStorageConfigDto().apply {
        this.connectionString = connectionString
        this.containerName = "container"
      }
    AzureFileStorageFactory().create(config)
  }

  companion object {
    private const val AZURITE_CONNECTION_STRING =
      "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
        "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
        "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
  }
}
