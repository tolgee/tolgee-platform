package io.tolgee.formats.apple.`in`

fun guessLanguageFromPath(filePath: String): String {
  return filePath.split("/").find { it.endsWith(".lproj") }?.removeSuffix(".lproj") ?: "unknown"
}
