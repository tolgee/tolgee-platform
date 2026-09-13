/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.util

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.exceptions.FileStoreException

class InMemoryFileStorage : FileStorage {
  private val files = mutableMapOf<String, ByteArray>()

  override fun readFile(storageFilePath: String): ByteArray {
    return files[storageFilePath]
      ?: throw FileStoreException("File not found", storageFilePath)
  }

  override fun deleteFile(storageFilePath: String) {
    files.remove(storageFilePath)
  }

  override fun storeFile(
    storageFilePath: String,
    bytes: ByteArray,
  ) {
    files[storageFilePath] = bytes
  }

  override fun fileExists(storageFilePath: String): Boolean {
    return files.contains(storageFilePath)
  }

  override fun pruneDirectory(path: String) {
    val keysToDelete = files.keys.filter { it.startsWith(path.removeSuffix("/") + "/") }
    keysToDelete.forEach {
      files.remove(it)
    }
  }

  fun clear() {
    files.clear()
  }
}
