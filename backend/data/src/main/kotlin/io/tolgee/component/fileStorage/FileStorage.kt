package io.tolgee.component.fileStorage

interface FileStorage {
  fun readFile(storageFilePath: String): ByteArray
  fun deleteFile(storageFilePath: String)
  fun storeFile(storageFilePath: String, bytes: ByteArray)
  fun fileExists(storageFilePath: String): Boolean
}
