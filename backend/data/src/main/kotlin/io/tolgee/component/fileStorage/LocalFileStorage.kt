/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component.fileStorage

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.FileStoreException
import java.io.File

class LocalFileStorage(
  tolgeeProperties: TolgeeProperties,
) : FileStorage {
  private val localDataPath = tolgeeProperties.fileStorage.fsDataPath

  override fun readFile(storageFilePath: String): ByteArray {
    try {
      return getLocalFile(storageFilePath).readBytes()
    } catch (e: Exception) {
      throw FileStoreException("Can not obtain file", storageFilePath, e)
    }
  }

  override fun deleteFile(storageFilePath: String) {
    try {
      getLocalFile(storageFilePath).delete()
    } catch (e: Exception) {
      throw FileStoreException("Can not delete file from local filesystem!", storageFilePath, e)
    }
  }

  override fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
  ) {
    val file = getLocalFile(storageFilePath)
    try {
      file.parentFile.mkdirs()
      file.writeBytes(bytes)
    } catch (e: Exception) {
      throw FileStoreException("Can not store file to local filesystem!", storageFilePath, e)
    }
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return getLocalFile(storageFilePath).exists()
  }

  private fun getLocalFile(storageFilePath: String): File {
    return File("$localDataPath/$storageFilePath")
  }
}
