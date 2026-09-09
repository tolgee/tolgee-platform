package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import java.net.URI

/**
 * Validates and resolves one manifest `icon` value in a single pass. An emoji or a native icon
 * name passes through as-is; anything containing a slash is an image URL, resolved against the base
 * URL (so a relative path becomes absolute) and required to be absolute http(s). A cross-origin
 * image URL is allowed — the browser loading a third-party `<img>` is an ordinary, safe primitive.
 * A slash-free value must not carry a URI scheme or markup, or `javascript:x` / `<img onerror=…>`
 * would be stored as an "emoji".
 *
 * The checks run once and cache their result, so the manifest validator (which reads [collectErrors])
 * and the fetcher (which reads [resolve]) share one instance and never recompute or diverge.
 */
class AppIconResolver(
  rawIcon: String?,
  private val baseUrl: String,
) {
  private val icon = rawIcon?.trim()
  private val errors = mutableListOf<String>()
  private var resolved: String? = null
  private var validated = false

  /** Problems with the icon, empty when it is valid. */
  fun collectErrors(): List<String> {
    validate()
    return errors
  }

  /** The stored form of the icon. Throws [BadRequestException] if the icon is invalid. */
  fun resolve(): String? {
    validate()
    if (errors.isNotEmpty()) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID, errors)
    }
    return resolved
  }

  private fun validate() {
    if (validated) return
    validated = true
    val value = icon
    if (value.isNullOrEmpty()) return
    if (isTooLong(value)) return
    if (value.contains('/')) {
      resolved = resolveImageUrl(value)
      return
    }
    if (isInvalidSymbol(value)) {
      errors.add("must be an emoji, a native icon name, or an image URL")
      return
    }
    resolved = value
  }

  private fun isTooLong(value: String): Boolean {
    if (value.length <= MAX_ICON_LENGTH) return false
    errors.add("exceeds $MAX_ICON_LENGTH characters")
    return true
  }

  private fun isInvalidSymbol(value: String): Boolean {
    if (value.contains(':')) return true
    if (value.contains('<') || value.contains('>')) return true
    return value.any { it.isWhitespace() }
  }

  private fun resolveImageUrl(value: String): String? {
    val resolvedUri =
      try {
        URI(baseUrl).resolve(value)
      } catch (e: Exception) {
        errors.add("is not a valid URL: ${e.message}")
        return null
      }
    if (resolvedUri.scheme?.lowercase() !in HTTP_SCHEMES || resolvedUri.host.isNullOrBlank()) {
      errors.add("must resolve to an absolute http(s) URL")
      return null
    }
    return resolvedUri.toString()
  }

  companion object {
    private const val MAX_ICON_LENGTH = 500
    private val HTTP_SCHEMES = setOf("http", "https")
  }
}
