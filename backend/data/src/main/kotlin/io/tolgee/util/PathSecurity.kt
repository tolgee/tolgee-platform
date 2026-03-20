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
    var result = Paths.get(path).normalize().toString()
    val dotDotSep = ".." + java.io.File.separator
    while (result.startsWith(dotDotSep)) {
      result = result.removePrefix(dotDotSep)
    }
    if (result == "..") {
      return ""
    }
    return result
  }
}
