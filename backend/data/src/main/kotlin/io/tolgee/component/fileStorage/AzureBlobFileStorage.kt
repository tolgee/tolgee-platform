/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.BlobContainerClient
import io.tolgee.exceptions.FileStoreException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.lang.reflect.Field

open class AzureBlobFileStorage(
  private val client: BlobContainerClient,
) : FileStorage {
  override fun readFile(storageFilePath: String): ByteArray {
    try {
      return getBlobClient(storageFilePath).downloadContent().toBytes()
    } catch (e: Exception) {
      throw FileStoreException("Can not obtain file", storageFilePath, e)
    }
  }

  override fun deleteFile(storageFilePath: String) {
    try {
      getBlobClient(storageFilePath).delete()
      return
    } catch (e: Exception) {
      throw FileStoreException("Can not delete file using Azure Blob!", storageFilePath, e)
    }
  }

  override fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
  ) {
    try {
      val client = getBlobClient(storageFilePath)
      client.upload(BinaryData.fromBytes(bytes), true)
    } catch (e: Exception) {
      throw FileStoreException("Can not store file using Azure Blob!", storageFilePath, e)
    }
    return
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return try {
      getBlobClient(storageFilePath).exists()
      true
    } catch (e: NoSuchKeyException) {
      false
    }
  }

  private fun getBlobClient(storageFilePath: String): BlobClient {
    val clientValue = extractClientFromBlobContainerClient()
    return TolgeeBlobClient(clientValue.getBlobAsyncClient(storageFilePath))
  }

  /**
   * The Azure blob client uses deprecated method:
   * reactor.core.publisher.Mono reactor.core.publisher.Mono.subscriberContext
   * It was removed in mono 3.5 (minor version ugh!)
   *
   * This is a workaround for this issue.
   */
  private fun extractClientFromBlobContainerClient(): BlobContainerAsyncClient {
    val clientField: Field? = BlobContainerClient::class.java.getDeclaredField("client")
    clientField?.isAccessible = true
    return clientField?.get(client) as BlobContainerAsyncClient
  }
}
