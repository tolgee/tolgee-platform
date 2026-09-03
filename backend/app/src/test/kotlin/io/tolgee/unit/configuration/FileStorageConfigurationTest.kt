package io.tolgee.unit.configuration

import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.LocalFileStorage
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.FileStorageConfiguration
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.fixtures.AzuriteRunner
import io.tolgee.testing.assert
import io.tolgee.util.InMemoryFileStorage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FileStorageConfigurationTest {
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

  @Test
  fun `fails when azure is enabled without connection string`() {
    enableAzure()
    properties.fileStorage.azure.connectionString = null
    assertThrows<IllegalStateException> { fileStorage() }
      .message
      .assert
      .contains("connection-string")
  }

  @Test
  fun `fails when azure is enabled without container name`() {
    enableAzure()
    properties.fileStorage.azure.containerName = ""
    assertThrows<IllegalStateException> { fileStorage() }
  }

  private fun fileStorage() =
    FileStorageConfiguration(properties, S3FileStorageFactory(), AzureFileStorageFactory()).fileStorage()

  private fun enableAzure() {
    properties.fileStorage.azure.apply {
      enabled = true
      connectionString = AzuriteRunner.connectionString
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
