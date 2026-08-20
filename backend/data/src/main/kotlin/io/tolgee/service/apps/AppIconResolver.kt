package io.tolgee.service.apps

import org.springframework.stereotype.Component
import java.net.URI

/**
 * Resolves a manifest's `icon` into what gets stored: an emoji or a native icon name passes
 * through as-is; anything containing a slash is an image URL and is resolved to absolute http(s)
 * against the base URL — the browser will load it from the app's host directly, so a broken or
 * non-http value must be refused here. A slash-free value must not carry a URI scheme either, or
 * `javascript:x` would be stored as an "emoji".
 *
 * An image URL must stay on the app's own origin. Tolgee never fetches the icon — the admin's
 * browser does, in an `<img>` with no SSRF guard and no CSP restricting `img-src`. An off-origin
 * URL would let a registered app fire a stored, blind GET from every viewing admin's browser at
 * an arbitrary host (an internal LAN device, a tracking beacon). Confining it to the origin the
 * admin already trusts by installing the app removes that.
 */
@Component
class AppIconResolver {
  fun validate(
    icon: String?,
    baseUrl: String,
    errors: MutableList<String>,
    field: String = "icon",
  ) {
    resolveInternal(icon, baseUrl).error?.let { errors.add("$field $it") }
  }

  /** The stored form of a valid icon. Call after [validate] passed. */
  fun resolve(
    icon: String?,
    baseUrl: String,
  ): String? = resolveInternal(icon, baseUrl).value

  private fun resolveInternal(
    rawIcon: String?,
    baseUrl: String,
  ): Resolution {
    val icon = rawIcon?.trim()
    if (icon.isNullOrEmpty()) return Resolution()
    if (icon.length > MAX_ICON_LENGTH) {
      return Resolution(error = "exceeds $MAX_ICON_LENGTH characters")
    }
    if (!icon.contains('/')) {
      if (icon.contains(':')) {
        return Resolution(error = "must be an emoji, a native icon name, or an image URL")
      }
      return Resolution(value = icon)
    }
    val resolved =
      try {
        URI(baseUrl).resolve(icon)
      } catch (e: Exception) {
        return Resolution(error = "is not a valid URL: ${e.message}")
      }
    if (resolved.scheme?.lowercase() !in HTTP_SCHEMES || resolved.host.isNullOrBlank()) {
      return Resolution(error = "must resolve to an absolute http(s) URL")
    }
    val base = parseHttpUri(baseUrl)
    if (base == null || originOf(resolved) != originOf(base)) {
      return Resolution(error = "must be on the app's own origin")
    }
    return Resolution(value = resolved.toString())
  }

  private fun parseHttpUri(value: String): URI? {
    val uri =
      try {
        URI(value)
      } catch (e: Exception) {
        return null
      }
    if (uri.scheme?.lowercase() !in HTTP_SCHEMES || uri.host.isNullOrBlank()) return null
    return uri
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

  private data class Resolution(
    val value: String? = null,
    val error: String? = null,
  )

  companion object {
    private const val MAX_ICON_LENGTH = 500
    private val HTTP_SCHEMES = setOf("http", "https")
  }
}
