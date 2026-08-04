/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.azure.core.util.BinaryData
import com.azure.core.util.Context
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.models.ListBlobsOptions
import com.azure.storage.blob.options.BlobParallelUploadOptions
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
    } catch (e: Exception) {
      throw FileStoreException("Can not delete file using Azure Blob!", storageFilePath, e)
    }
  }

  override fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
    contentType: String?,
  ) {
    try {
      val options = BlobParallelUploadOptions(BinaryData.fromBytes(bytes))
      contentType?.let { options.setHeaders(BlobHttpHeaders().setContentType(it)) }
      client.getBlobClient(storageFilePath).uploadWithResponse(options, null, Context.NONE)
    } catch (e: Exception) {
      throw FileStoreException("Can not store file using Azure Blob!", storageFilePath, e)
    }
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
