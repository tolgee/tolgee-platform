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

  override fun pruneDirectory(path: String) {
    try {
      val dir = getLocalFile(path)
      if (dir.isDirectory) {
        dir.listFiles()?.forEach {
          it.deleteRecursively()
        }
      }
    } catch (e: Exception) {
      throw FileStoreException("Cannot prune directory: $path", path, e)
    }
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return getLocalFile(storageFilePath).exists()
  }

  private fun getLocalFile(storageFilePath: String): File {
    val dataRoot = localDataPath.removeTrailingSlash()
    val normalizedFilePath = storageFilePath.removeLeadingSlash()
    return File("$dataRoot/$normalizedFilePath")
  }

  private fun String.removeLeadingSlash() = this.removePrefix("/")

  private fun String.removeTrailingSlash() = this.removeSuffix("/")
}
