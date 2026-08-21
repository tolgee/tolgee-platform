package io.tolgee.service.apps

import java.net.URI

/**
 * Validates and resolves one manifest `icon` value. An emoji or a native icon name passes through
 * as-is; anything containing a slash is an image URL and is resolved to absolute http(s) against
 * the base URL — the browser loads it from the app's host directly, so a broken or non-http value
 * must be refused here. A slash-free value must not carry a URI scheme either, or `javascript:x`
 * would be stored as an "emoji".
 *
 * An image URL must stay on the app's own origin. Tolgee never fetches the icon — the admin's
 * browser does, in an `<img>` with no SSRF guard and no CSP restricting `img-src`. An off-origin
 * URL would let a registered app fire a stored, blind GET from every viewing admin's browser at
 * an arbitrary host (an internal LAN device, a tracking beacon). Confining it to the origin the
 * admin already trusts by installing the app removes that.
 *
 * A new instance validates a single icon; [collectErrors] returns what it found and the caller
 * decides how to prefix and surface it, so this class never touches the caller's error list.
 */
class AppIconResolver(
  rawIcon: String?,
  private val baseUrl: String,
) {
  private val icon = rawIcon?.trim()
  private val errors = mutableListOf<String>()

  /** Problems with the icon, empty when it is valid. */
  fun collectErrors(): List<String> {
    val value = icon
    if (value.isNullOrEmpty()) return errors
    if (isTooLong(value)) return errors
    if (!value.contains('/')) {
      validateSymbol(value)
      return errors
    }
    validateImageUrl(value)
    return errors
  }

  /** The stored form of a valid icon. Call only after [collectErrors] returned empty. */
  fun resolve(): String? {
    val value = icon
    if (value.isNullOrEmpty()) return null
    if (!value.contains('/')) return value
    return resolveImageUrl(value)?.toString()
  }

  private fun isTooLong(value: String): Boolean {
    if (value.length <= MAX_ICON_LENGTH) return false
    errors.add("exceeds $MAX_ICON_LENGTH characters")
    return true
  }

  private fun validateSymbol(value: String) {
    if (value.contains(':')) {
      errors.add("must be an emoji, a native icon name, or an image URL")
    }
  }

  private fun validateImageUrl(value: String) {
    val resolved = resolveImageUrl(value) ?: return
    if (originOf(resolved) != baseOrigin()) {
      errors.add("must be on the app's own origin")
    }
  }

  private fun resolveImageUrl(value: String): URI? {
    val resolved =
      try {
        URI(baseUrl).resolve(value)
      } catch (e: Exception) {
        errors.add("is not a valid URL: ${e.message}")
        return null
      }
    if (resolved.scheme?.lowercase() !in HTTP_SCHEMES || resolved.host.isNullOrBlank()) {
      errors.add("must resolve to an absolute http(s) URL")
      return null
    }
    return resolved
  }

  private fun baseOrigin(): String? {
    val base =
      try {
        URI(baseUrl)
      } catch (e: Exception) {
        return null
      }
    return originOf(base)
  }

  private fun originOf(uri: URI): String? {
    val host = uri.host?.lowercase() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    val port = if (uri.port == -1) defaultPortOf(scheme) else uri.port
    return "$scheme://$host:$port"
  }

  private fun defaultPortOf(scheme: String): Int {
    if (scheme == "https") return 443
    return 80
  }

  companion object {
    private const val MAX_ICON_LENGTH = 500
    private val HTTP_SCHEMES = setOf("http", "https")
  }
}
