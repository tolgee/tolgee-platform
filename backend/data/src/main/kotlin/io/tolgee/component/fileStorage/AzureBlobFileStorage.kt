/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import io.tolgee.exceptions.FileStoreException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

class AzureBlobFileStorage(
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

  override fun storeFile(storageFilePath: String, bytes: ByteArray) {
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

  private fun getBlobClient(storageFilePath: String): BlobClient =
    client.getBlobClient(storageFilePath)
}
