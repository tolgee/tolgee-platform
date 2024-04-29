/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.tolgee.exceptions.FileStoreException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import java.io.ByteArrayInputStream

open class S3FileStorage(
  private val bucketName: String,
  private val s3: S3Client,
) : FileStorage {
  override fun readFile(storageFilePath: String): ByteArray {
    try {
      return s3.getObject { b -> b.bucket(bucketName).key(storageFilePath) }.readAllBytes()
    } catch (e: Exception) {
      throw FileStoreException("Can not obtain file", storageFilePath, e)
    }
  }

  override fun deleteFile(storageFilePath: String) {
    try {
      s3.deleteObject { b -> b.bucket(bucketName).key(storageFilePath) }
      return
    } catch (e: Exception) {
      throw FileStoreException("Can not delete file using s3 bucket!", storageFilePath, e)
    }
  }

  override fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
  ) {
    val byteArrayInputStream = ByteArrayInputStream(bytes)
    try {
      s3.putObject(
        { b -> b.bucket(bucketName).key(storageFilePath) },
        RequestBody.fromInputStream(byteArrayInputStream, bytes.size.toLong()),
      )
    } catch (e: Exception) {
      throw FileStoreException("Can not store file using s3 bucket!", storageFilePath, e)
    }
    return
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return try {
      s3.headObject { b -> b.bucket(bucketName).key(storageFilePath) }
      true
    } catch (e: NoSuchKeyException) {
      false
    }
  }

  override fun pruneDirectory(path: String) {
    try {
      // Add trailing slash if not exists
      val adjustedPath = if (path.endsWith("/")) path else "$path/"

      val objectsToDelete =
        s3.listObjectsV2 { it.bucket(bucketName).prefix(adjustedPath) }
          .contents()
          .map { it.key() }
          .toSet()
          .map { ObjectIdentifier.builder().key(it).build() }

      if (objectsToDelete.isNotEmpty()) {
        val deleteObjectsRequest =
          DeleteObjectsRequest.builder()
            .bucket(bucketName)
            .delete {
              it.objects(objectsToDelete)
            }
            .build()

        s3.deleteObjects(deleteObjectsRequest)
      }
    } catch (e: Exception) {
      throw FileStoreException("Can not prune directory in s3 bucket!", path, e)
    }
  }
}
