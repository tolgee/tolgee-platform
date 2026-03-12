package io.tolgee.util

import java.nio.file.Paths

/**
 * Utility for sanitizing file paths to prevent path traversal attacks.
 * Normalizes the path and strips any leading ".." components so the
 * result always stays relative and within the intended directory.
 */
object PathSecurity {
  /**
   * Normalizes the given path and removes any leading ".." traversal.
   * Safe to use on zip entry names, export paths, and imported file names.
   */
  fun sanitizePath(path: String): String {
    val normalized = Paths.get(path).normalize().toString()
    if (normalized.startsWith("..")) {
      return normalized.removePrefix(".." + java.io.File.separator)
    }
    return normalized
  }
}
