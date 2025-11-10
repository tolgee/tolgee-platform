/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.http.rest.PagedIterable
import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobItem
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FileStorageAzureTest {
  private lateinit var azureFs: AzureBlobFileStorage
  private lateinit var containerClientMock: BlobContainerClient
  private val filePath = "/hello/hello/en.json"
  private val content = "hello"
  private lateinit var blobClientMock: BlobClient

  @BeforeEach
  fun setup() {
    containerClientMock = mock()
    azureFs = AzureBlobFileStorage(containerClientMock)
    blobClientMock = mock()
    whenever(containerClientMock.getBlobClient(eq(filePath))).then { blobClientMock }
    val binaryDataMock = mock<BinaryData>()
    whenever(blobClientMock.downloadContent()).thenReturn(binaryDataMock)
    whenever(binaryDataMock.toBytes()).thenReturn(content.toByteArray(Charsets.UTF_8))
  }

  @Test
  fun testGetFile() {
    azureFs
      .readFile(filePath)
      .toString(Charsets.UTF_8)
      .assert
      .isEqualTo("hello")
    verifyGetsClient()
  }

  @Test
  fun testDeleteFile() {
    azureFs.deleteFile(filePath)
    verify(blobClientMock, times(1)).delete()
    verifyGetsClient()
  }

  @Test
  fun testStoreFile() {
    val bytes = content.toByteArray(Charsets.UTF_8)
    azureFs.storeFile(filePath, bytes)
    verify(blobClientMock, times(1)).upload(any<BinaryData>(), eq(true))
    val binaryData =
      Mockito
        .mockingDetails(blobClientMock)
        .invocations
        .single()
        .arguments[0] as BinaryData
    binaryData
      .toBytes()
      .toString(Charsets.UTF_8)
      .assert
      .isEqualTo(content)
    verifyGetsClient()
  }

  @Test
  fun testPruneDirectory() {
    val pagedIterableMock = mock<PagedIterable<BlobItem>>()
    whenever(containerClientMock.listBlobs(any(), eq(null))).thenReturn(pagedIterableMock)
    whenever(pagedIterableMock.iterator()).thenReturn(
      mutableListOf(
        BlobItem().apply {
          name = filePath
        },
      ).iterator(),
    )
    azureFs.pruneDirectory("hello")
    verifyGetsClient()
    verify(blobClientMock, times(1)).delete()
  }

  @Test
  fun testFileExists() {
    whenever(blobClientMock.exists()).thenReturn(true)
    azureFs.fileExists(filePath).assert.isTrue()
    verifyGetsClient()
    verify(blobClientMock, times(1)).exists()
  }

  private fun verifyGetsClient() {
    verify(containerClientMock, times(1)).getBlobClient(eq(filePath))
  }
}
