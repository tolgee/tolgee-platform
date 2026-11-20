package io.tolgee.unit.component.contentDelivery

import io.tolgee.component.contentDelivery.ContentDeliveryFileStorageProvider
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.configuration.tolgee.ContentStorageS3Properties
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ContentDeliveryFileStorageProviderTest {
  private val properties = TolgeeProperties()
  private val s3FileStorageFactory = mock<S3FileStorageFactory>()
  private val provider = ContentDeliveryFileStorageProvider(properties, s3FileStorageFactory, mock())

  @Test
  fun `the server default storage keeps the unpinned client`() {
    properties.contentDelivery.storage.s3.bucketName = "server-bucket"

    provider.getContentStorageWithDefaultClient()

    verify(s3FileStorageFactory).create(properties.contentDelivery.storage.s3)
    verify(s3FileStorageFactory, never()).createSsrfSafe(any(), any())
  }

  @Test
  fun `a project storage gets the pinned client with the configured local-address policy`() {
    val projectStorage = ContentStorageS3Properties(bucketName = "project-bucket")

    provider.getStorage(projectStorage)
    properties.contentDelivery.storage.allowLocalAddresses = true
    provider.getStorage(projectStorage)

    verify(s3FileStorageFactory).createSsrfSafe(projectStorage, allowLocalAddresses = false)
    verify(s3FileStorageFactory).createSsrfSafe(projectStorage, allowLocalAddresses = true)
  }
}
