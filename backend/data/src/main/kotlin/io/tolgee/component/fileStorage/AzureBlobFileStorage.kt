/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.ListBlobsOptions
import io.tolgee.exceptions.FileStoreException

open class AzureBlobFileStorage(
  private val client: BlobContainerClient,
) : FileStorage {
  override fun readFile(storageFilePath: String): ByteArray {
    try {
      return client.getBlobClient(storageFilePath).downloadContent().toBytes()
    } catch (e: Exception) {
      throw FileStoreException("Can not obtain file", storageFilePath, e)
    }
  }

  override fun deleteFile(storageFilePath: String) {
    try {
      client.getBlobClient(storageFilePath).deleteIfExists()
    } catch (e: Exception) {
      throw FileStoreException("Can not delete file using Azure Blob!", storageFilePath, e)
    }
  }

  override fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
  ) {
    try {
      client.getBlobClient(storageFilePath).upload(BinaryData.fromBytes(bytes), true)
    } catch (e: Exception) {
      throw FileStoreException("Can not store file using Azure Blob!", storageFilePath, e)
    }
  }

  override fun fileExists(storageFilePath: String): Boolean {
    try {
      return client.getBlobClient(storageFilePath).exists()
    } catch (e: Exception) {
      throw FileStoreException("Can not check file existence using Azure Blob!", storageFilePath, e)
    }
  }

  override fun pruneDirectory(path: String) {
    val prefix = path.removePrefix("/").removeSuffix("/") + "/"
    val options = ListBlobsOptions()
    options.prefix = prefix
    try {
      client.listBlobs(options, null).forEach {
        client.getBlobClient(it.name).delete()
      }
    } catch (e: Exception) {
      throw FileStoreException("Can not prune directory using Azure Blob!", path, e)
    }
  }
}
