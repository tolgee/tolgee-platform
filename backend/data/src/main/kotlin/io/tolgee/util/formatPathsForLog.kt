package io.tolgee.util

private const val MAX_LOGGED_PATHS = 3
private const val MAX_LOGGED_PATH_LENGTH = 200
private val LOG_UNSAFE_CHARACTERS_REGEX = "[\\p{C}\\u2028\\u2029]".toRegex()

/** Paths may be user-authored, so they are scrubbed of non-printables and truncated before reaching a log. */
fun formatPathsForLog(paths: List<String>): String {
  val shown =
    paths
      .take(MAX_LOGGED_PATHS)
      .joinToString { it.replace(LOG_UNSAFE_CHARACTERS_REGEX, "_").take(MAX_LOGGED_PATH_LENGTH) }
  if (paths.size <= MAX_LOGGED_PATHS) {
    return shown
  }
  return "$shown and ${paths.size - MAX_LOGGED_PATHS} more"
}
