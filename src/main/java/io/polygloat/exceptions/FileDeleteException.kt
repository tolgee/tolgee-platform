/*
 * Copyright (c) 2020. Polygloat
 */

package io.polygloat.exceptions

class FileDeleteException(message: String, val storageFilePath: String, val e: Exception? = null) : RuntimeException(message, e)
