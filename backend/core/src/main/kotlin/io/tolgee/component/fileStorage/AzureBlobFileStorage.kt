/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.ListBlobsOptions
import io.tolgee.exceptions.FileStoreException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

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
      client.getBlobClient(storageFilePath).delete()
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
      val client = client.getBlobClient(storageFilePath)
      client.upload(BinaryData.fromBytes(bytes), true)
    } catch (e: Exception) {
      throw FileStoreException("Can not store file using Azure Blob!", storageFilePath, e)
    }
    return
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return try {
      client.getBlobClient(storageFilePath).exists()
      true
    } catch (e: NoSuchKeyException) {
      false
    }
  }

  override fun pruneDirectory(path: String) {
    val prefix = path.removePrefix("/").removeSuffix("/") + "/"
    val options = ListBlobsOptions()
    options.prefix = prefix
    client.listBlobs(options, null).forEach {
      client.getBlobClient(it.name).delete()
    }
  }
}
