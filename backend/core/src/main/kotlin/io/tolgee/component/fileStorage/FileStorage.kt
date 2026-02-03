package io.tolgee.component.fileStorage

import io.tolgee.exceptions.FileStoreException

interface FileStorage {
  fun readFile(storageFilePath: String): ByteArray

  fun deleteFile(storageFilePath: String)

  fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
  )

  fun fileExists(storageFilePath: String): Boolean

  fun pruneDirectory(path: String)

  fun test() {
    try {
      this.storeFile("test", "test".toByteArray())
      this.readFile("test")
      this.deleteFile("test")
    } catch (e: Exception) {
      throw FileStoreException("Storage test failed", "test", e)
    }
  }
}
