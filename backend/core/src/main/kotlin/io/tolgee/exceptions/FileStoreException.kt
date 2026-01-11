/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.exceptions

class FileStoreException(
  message: String,
  val storageFilePath: String,
  val e: Exception? = null,
) : RuntimeException(message, e)
