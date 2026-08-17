/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.http.rest.PagedIterable
import com.azure.core.util.BinaryData
import com.azure.core.util.Context
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.options.BlobParallelUploadOptions
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FileStorageAzureTest {
  private lateinit var azureFs: AzureBlobFileStorage
  private lateinit var containerClientMock: BlobContainerClient
  private val filePath = "/hello/hello/en.json"
  private val content = "hello"
  private val contentBytes = content.toByteArray(Charsets.UTF_8)
  private lateinit var blobClientMock: BlobClient

  @BeforeEach
  fun setup() {
    containerClientMock = mock()
    azureFs = AzureBlobFileStorage(containerClientMock)
    blobClientMock = mock()
    whenever(containerClientMock.getBlobClient(eq(filePath))).then { blobClientMock }
    val binaryDataMock = mock<BinaryData>()
    whenever(blobClientMock.downloadContent()).thenReturn(binaryDataMock)
    whenever(binaryDataMock.toBytes()).thenReturn(contentBytes)
  }

  @Test
  fun testGetFile() {
    azureFs
      .readFile(filePath)
      .toString(Charsets.UTF_8)
      .assert
      .isEqualTo(content)
    verifyGetsClient()
  }

  @Test
  fun testDeleteFile() {
    azureFs.deleteFile(filePath)
    verify(blobClientMock, times(1)).delete()
    verifyGetsClient()
  }

  @Test
  fun `stores file without a content type`() {
    azureFs.storeFile(filePath, contentBytes)

    val options = captureUploadOptions()
    options.uploadedContent().assert.isEqualTo(content)
    options.headers.assert.isNull()
    options.assertOverwriteAllowed()
    verifyGetsClient()
  }

  @Test
  fun `stores file with content type`() {
    azureFs.storeFile(filePath, contentBytes, "application/json")

    val options = captureUploadOptions()
    options.uploadedContent().assert.isEqualTo(content)
    options.headers
      ?.contentType.assert
      .isEqualTo("application/json")
    options.assertOverwriteAllowed()
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

  private fun captureUploadOptions(): BlobParallelUploadOptions {
    val captor = argumentCaptor<BlobParallelUploadOptions>()
    verify(blobClientMock, times(1)).uploadWithResponse(captor.capture(), eq(null), eq(Context.NONE))
    return captor.firstValue
  }

  private fun BlobParallelUploadOptions.uploadedContent() =
    BinaryData
      .fromFlux(this.dataFlux)
      .block()!!
      .toBytes()
      .toString(Charsets.UTF_8)

  private fun BlobParallelUploadOptions.assertOverwriteAllowed() {
    this.requestConditions
      ?.ifNoneMatch.assert
      .describedAs("an ifNoneMatch condition would make republishing fail with BlobAlreadyExists")
      .isNull()
  }
}
