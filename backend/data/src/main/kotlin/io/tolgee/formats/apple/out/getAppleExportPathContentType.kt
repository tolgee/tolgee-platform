package io.tolgee.formats.apple.out

import io.tolgee.formats.apple.APPLE_STRINGSDICT_EXTENSION
import io.tolgee.formats.apple.APPLE_STRINGS_EXTENSION

private val APPLE_CONTENT_TYPES =
  mapOf(
    APPLE_STRINGSDICT_EXTENSION to "application/xml",
    APPLE_STRINGS_EXTENSION to "text/plain",
  )

private val APPLE_EXTENSION_ANYWHERE_REGEX =
  (
    "(?<![A-Za-z0-9])" +
      "(?:${Regex.escape(APPLE_STRINGSDICT_EXTENSION)}|${Regex.escape(APPLE_STRINGS_EXTENSION)})" +
      "(?![A-Za-z0-9])"
  ).toRegex()

fun getAppleExportPathContentType(exportRelativePath: String): String? {
  val segments = exportRelativePath.split('/')
  val fileName = segments.last()

  fileName.appleContentTypeFromDotSegments()?.let { return it }
  fileName.appleContentTypeFromWordAnywhere()?.let { return it }

  val directoriesNearestFirst = segments.dropLast(1).asReversed()
  val directoryMatches = directoriesNearestFirst.mapNotNull { it.appleContentTypeFromDotSegments() }.distinct()
  if (directoryMatches.size > 1) {
    return null
  }

  return directoryMatches.singleOrNull()
    ?: directoriesNearestFirst.firstNotNullOfOrNull { it.appleContentTypeFromWordAnywhere() }
}

private fun String.appleContentTypeFromDotSegments(): String? =
  this.split('.').asReversed().firstNotNullOfOrNull { APPLE_CONTENT_TYPES[it] }

private fun String.appleContentTypeFromWordAnywhere(): String? =
  APPLE_EXTENSION_ANYWHERE_REGEX
    .findAll(this)
    .lastOrNull()
    ?.let { APPLE_CONTENT_TYPES[it.value] }
