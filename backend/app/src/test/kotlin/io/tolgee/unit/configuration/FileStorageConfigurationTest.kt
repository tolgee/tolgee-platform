package io.tolgee.unit.configuration

import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.LocalFileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.FileStorageConfiguration
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.InvalidConnectionStringException
import io.tolgee.testing.assert
import io.tolgee.util.InMemoryFileStorage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class FileStorageConfigurationTest {
  companion object {
    const val WELL_FORMED_CONNECTION_STRING =
      "DefaultEndpointsProtocol=http;AccountName=unit;AccountKey=dGVzdA==;BlobEndpoint=http://127.0.0.1:1/unit;"
  }

  private val properties = TolgeeProperties()

  @Test
  fun `defaults to local storage`() {
    fileStorage().assert.isInstanceOf(LocalFileStorage::class.java)
  }

  @Test
  fun `in-memory storage wins over azure`() {
    properties.internal.useInMemoryFileStorage = true
    enableAzure()
    fileStorage().assert.isInstanceOf(InMemoryFileStorage::class.java)
  }

  @Test
  fun `selects azure when enabled`() {
    enableAzure()
    fileStorage().assert.isInstanceOf(AzureBlobFileStorage::class.java)
  }

  @Test
  fun `selects s3 when enabled`() {
    enableS3()
    fileStorage().assert.isInstanceOf(S3FileStorage::class.java)
  }

  @Test
  fun `fails when both s3 and azure are enabled`() {
    enableS3()
    enableAzure()
    assertThrows<IllegalStateException> { fileStorage() }
      .message
      .assert
      .contains("exactly one")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [" "])
  fun `fails when azure is enabled without connection string`(blank: String?) {
    enableAzure()
    properties.fileStorage.azure.connectionString = blank
    assertThrows<IllegalStateException> { fileStorage() }
      .message
      .assert
      .contains("connection-string is not set")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [" "])
  fun `fails when azure is enabled without container name`(blank: String?) {
    enableAzure()
    properties.fileStorage.azure.containerName = blank
    assertThrows<IllegalStateException> { fileStorage() }
      .message
      .assert
      .contains("container-name is not set")
  }

  @Test
  fun `names the azure properties and keeps the SDK cause when the connection string is malformed`() {
    enableAzure()
    properties.fileStorage.azure.connectionString = "not-a-connection-string"
    val exception = assertThrows<IllegalStateException> { fileStorage() }
    exception.message.assert.contains("tolgee.file-storage.azure")
    val factoryFailure = exception.cause
    factoryFailure.assert.isInstanceOf(InvalidConnectionStringException::class.java)
    val sdkFailure = factoryFailure?.cause
    sdkFailure.assert.isInstanceOf(IllegalArgumentException::class.java)
    sdkFailure?.message.assert.isEqualTo("Invalid connection string.")
  }

  private fun fileStorage() =
    FileStorageConfiguration(properties, S3FileStorageFactory(), AzureFileStorageFactory()).fileStorage()

  private fun enableAzure() {
    properties.fileStorage.azure.apply {
      enabled = true
      connectionString = WELL_FORMED_CONNECTION_STRING
      containerName = "container"
    }
  }

  private fun enableS3() {
    properties.fileStorage.s3.apply {
      enabled = true
      bucketName = "bucket"
      endpoint = "http://localhost:1"
      signingRegion = "us-east-1"
      accessKey = "access"
      secretKey = "secret"
    }
  }
}
