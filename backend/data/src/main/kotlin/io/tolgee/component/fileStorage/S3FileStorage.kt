/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.FileStoreException
import java.io.ByteArrayInputStream

class S3FileStorage(
  tolgeeProperties: TolgeeProperties,
  private val s3: AmazonS3,
) : FileStorage {

  private val bucketName = tolgeeProperties.fileStorage.s3.bucketName

  override fun readFile(storageFilePath: String): ByteArray {
    try {
      return s3.getObject(bucketName, storageFilePath).objectContent.readAllBytes()
    } catch (e: Exception) {
      throw FileStoreException("Can not obtain file", storageFilePath, e)
    }
  }

  override fun deleteFile(storageFilePath: String) {
    try {
      s3.deleteObject(bucketName, storageFilePath)
      return
    } catch (e: Exception) {
      throw FileStoreException("Can not delete file using s3 bucket!", storageFilePath, e)
    }
  }

  override fun storeFile(storageFilePath: String, bytes: ByteArray) {
    val byteArrayInputStream = ByteArrayInputStream(bytes)
    val meta = ObjectMetadata()
    meta.contentLength = bytes.size.toLong()
    val putObjectRequest = PutObjectRequest(
      bucketName,
      storageFilePath,
      byteArrayInputStream, meta
    )
    try {
      s3.putObject(putObjectRequest)
    } catch (e: Exception) {
      throw FileStoreException("Can not store file using s3 bucket!", storageFilePath, e)
    }
    return
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return s3.doesObjectExist(bucketName, storageFilePath)
  }
}
