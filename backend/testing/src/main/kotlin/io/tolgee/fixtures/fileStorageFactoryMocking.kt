package io.tolgee.fixtures

import io.tolgee.component.fileStorage.AzureBlobFileStorage
import io.tolgee.component.fileStorage.AzureFileStorageFactory
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

fun S3FileStorageFactory.mockCreatedStorage(): S3FileStorage {
  val mockedFileStorage = mock<S3FileStorage>()
  doAnswer { mockedFileStorage }.whenever(this).create(any())
  return mockedFileStorage
}

fun AzureFileStorageFactory.mockCreatedStorage(): AzureBlobFileStorage {
  val mockedFileStorage = mock<AzureBlobFileStorage>()
  doAnswer { mockedFileStorage }.whenever(this).create(any())
  return mockedFileStorage
}
