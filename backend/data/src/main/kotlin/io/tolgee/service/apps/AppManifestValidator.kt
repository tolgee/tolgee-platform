package io.tolgee.service.apps

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppManifestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI

/**
 * Validates everything a third-party manifest declares. Content problems are collected and thrown
 * together as one [Message.APP_MANIFEST_INVALID], so the app author fixes the manifest in one
 * round instead of one error at a time. The same-origin rejection keeps its own code — it is a
 * security refusal, not a fixable content error.
 */
@Component
class AppManifestValidator(
  private val tolgeeProperties: TolgeeProperties,
  private val appIconResolver: AppIconResolver,
) {
  fun validate(manifest: AppManifestDto) {
    val errors = mutableListOf<String>()
    validateStringFields(manifest, errors)
    validateUnsupportedFeatures(manifest, errors)
    validateScopes(manifest, errors)
    validateBaseUrl(manifest, errors)
    validateDashboardPages(manifest, errors)
    appIconResolver.validate(manifest.icon, manifest.baseUrl, errors)
    if (errors.isNotEmpty()) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID, errors)
    }
    rejectTolgeeOrigin(manifest)
  }

  /**
   * The columns backing these fields are `VARCHAR(255)`; without this an over-long value from a
   * third-party manifest would only fail at insert time, as a 500.
   */
  private fun validateStringFields(
    manifest: AppManifestDto,
    errors: MutableList<String>,
  ) {
    mapOf(
      "id" to manifest.id,
      "name" to manifest.name,
      "version" to manifest.version,
      "baseUrl" to manifest.baseUrl,
    ).forEach { (field, value) ->
      if (value.isBlank()) {
        errors.add("$field must not be blank")
      }
      if (value.length > MAX_FIELD_LENGTH) {
        errors.add("$field exceeds $MAX_FIELD_LENGTH characters")
      }
    }
  }

  /**
   * The alpha supports only the `project-dashboard-page` module. Any other top-level manifest key
   * (e.g. `webhooks`, `decoratorsUrl`) or module key is rejected here so the app author sees an
   * explicit error instead of a silently-ignored capability.
   */
  private fun validateUnsupportedFeatures(
    manifest: AppManifestDto,
    errors: MutableList<String>,
  ) {
    val unsupported = manifest.unknownProperties + manifest.modules.unknownModules
    if (unsupported.isNotEmpty()) {
      errors.add("unsupported manifest features: ${unsupported.sorted().joinToString(", ")}")
    }
  }

  private fun validateScopes(
    manifest: AppManifestDto,
    errors: MutableList<String>,
  ) {
    try {
      Scope.parse(manifest.scopes)
    } catch (e: BadRequestException) {
      errors.add("unknown scope: ${e.params?.firstOrNull() ?: ""}")
    }
  }

  private fun validateBaseUrl(
    manifest: AppManifestDto,
    errors: MutableList<String>,
  ) {
    if (parseAbsoluteHttpUrl(manifest.baseUrl) == null) {
      errors.add("baseUrl must be an absolute http(s) URL")
    }
  }

  private fun validateDashboardPages(
    manifest: AppManifestDto,
    errors: MutableList<String>,
  ) {
    val pages = manifest.modules.projectDashboardPage
    if (pages.isNullOrEmpty()) {
      errors.add("manifest must declare at least one project-dashboard-page module")
      return
    }

    val seenKeys = mutableSetOf<String>()
    pages.forEach { page ->
      mapOf(
        "key" to page.key,
        "title" to page.title,
        "icon" to page.icon,
        "entry" to page.entry,
      ).forEach { (field, value) ->
        if (value.isBlank()) {
          errors.add("project-dashboard-page $field must not be blank")
        }
      }
      if (!seenKeys.add(page.key)) {
        errors.add("duplicate project-dashboard-page key '${page.key}'")
      }
      validateEntry(manifest.baseUrl, page.key, page.entry, errors)
    }
  }

  /**
   * The entry is loaded in the app iframe, so it must stay on the app's own validated origin — an
   * absolute entry elsewhere would widen the iframe source beyond what anyone checked (including
   * to Tolgee's own origin, which only [rejectTolgeeOrigin]s the base URL).
   */
  private fun validateEntry(
    baseUrl: String,
    pageKey: String,
    entry: String,
    errors: MutableList<String>,
  ) {
    val base = parseAbsoluteHttpUrl(baseUrl) ?: return
    val resolved =
      try {
        base.resolve(entry)
      } catch (e: Exception) {
        errors.add("invalid project-dashboard-page '$pageKey' entry: ${e.message}")
        return
      }
    if (originOf(resolved) == null) {
      errors.add("project-dashboard-page '$pageKey' entry must resolve to an absolute http(s) URL")
      return
    }
    if (originOf(resolved) != originOf(base)) {
      errors.add("project-dashboard-page '$pageKey' entry must stay on the app's own origin")
    }
  }

  /**
   * The app iframe is sandboxed with `allow-scripts allow-same-origin`, which stops isolating an
   * app served from Tolgee's own origin: such an app shares the dashboard's `localStorage` and can
   * lift the signed-in user's JWT.
   *
   * When neither Tolgee URL property is configured (a default installation), the origin of the
   * current HTTP request stands in — during registration that is the origin Tolgee is actually
   * served on.
   */
  private fun rejectTolgeeOrigin(manifest: AppManifestDto) {
    val appOrigin = parseAbsoluteHttpUrl(manifest.baseUrl)?.let { originOf(it) } ?: return
    if (appOrigin in tolgeeOrigins()) {
      throw BadRequestException(Message.APP_MANIFEST_SAME_ORIGIN_AS_TOLGEE, listOf(appOrigin))
    }
  }

  private fun tolgeeOrigins(): List<String> {
    val configured =
      listOfNotNull(tolgeeProperties.frontEndUrl, tolgeeProperties.backEndUrl)
        .mapNotNull { url -> parseAbsoluteHttpUrl(url)?.let { originOf(it) } }
    return configured + listOfNotNull(currentRequestOrigin())
  }

  private fun currentRequestOrigin(): String? {
    val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
    val request = attributes.request
    return parseAbsoluteHttpUrl("${request.scheme}://${request.serverName}:${request.serverPort}")
      ?.let { originOf(it) }
  }

  private fun parseAbsoluteHttpUrl(value: String): URI? {
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

  companion object {
    private const val MAX_FIELD_LENGTH = 255
    private val HTTP_SCHEMES = setOf("http", "https")
  }
}
