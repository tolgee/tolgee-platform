package io.tolgee.service.apps

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import io.tolgee.util.UrlSecurity
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI

@Component
class AppManifestFetcher(
  private val appManifestHttpClient: AppManifestHttpClient,
  private val objectMapper: ObjectMapper,
  private val urlSecurity: UrlSecurity,
  private val appsProperties: AppsProperties,
  private val tolgeeProperties: TolgeeProperties,
) {
  data class FetchResult(
    val manifest: AppManifest,
    val rawJson: String,
    val scopes: Set<Scope>,
    /** The manifest's icon with a relative image URL already resolved against the base URL. */
    val icon: String? = null,
  )

  fun fetch(url: String): FetchResult {
    urlSecurity.validateUrl(url, allowLocalAddresses = appsProperties.allowLocalAddresses)

    val rawJson = appManifestHttpClient.fetchBody(url)

    val manifest =
      try {
        objectMapper.readValue<AppManifest>(rawJson)
      } catch (e: Exception) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf(e.message ?: ""))
      }

    val scopes =
      try {
        Scope.parse(manifest.scopes)
      } catch (e: BadRequestException) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("unknown scope: ${e.params?.firstOrNull() ?: ""}"),
        )
      }

    validateStringFields(manifest)
    rejectUnsupportedFeatures(manifest)
    validateDashboardPages(manifest)

    return FetchResult(
      manifest = manifest,
      rawJson = rawJson,
      scopes = scopes,
      icon = resolveIcon(manifest),
    )
  }

  /**
   * The columns backing these fields are `VARCHAR(255)`; without this an over-long value from a
   * third-party manifest would only fail at insert time, as a 500.
   */
  private fun validateStringFields(manifest: AppManifest) {
    mapOf(
      "id" to manifest.id,
      "name" to manifest.name,
      "version" to manifest.version,
      "baseUrl" to manifest.baseUrl,
    ).forEach { (field, value) ->
      if (value.isBlank()) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf("$field must not be blank"))
      }
      if (value.length > MAX_FIELD_LENGTH) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("$field exceeds $MAX_FIELD_LENGTH characters"),
        )
      }
    }
  }

  /**
   * The alpha supports only the `project-dashboard-page` module. Any other top-level manifest key
   * (e.g. `webhooks`, `decoratorsUrl`) or module key (tools panels, actions, …) is rejected here so
   * the app author sees an explicit error instead of a silently-ignored capability.
   */
  private fun rejectUnsupportedFeatures(manifest: AppManifest) {
    val unsupported = manifest.unknownProperties + manifest.modules.unknownModules
    if (unsupported.isNotEmpty()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("unsupported manifest features: ${unsupported.sorted().joinToString(", ")}"),
      )
    }
  }

  /**
   * The app iframe is sandboxed with `allow-scripts allow-same-origin`, which stops isolating an app
   * served from Tolgee's own origin: such an app shares the dashboard's `localStorage` and can lift
   * the signed-in user's JWT.
   */
  private fun rejectTolgeeOrigin(manifest: AppManifest) {
    val appOrigin = originOf(manifest.baseUrl) ?: return
    val tolgeeOrigins =
      listOfNotNull(tolgeeProperties.frontEndUrl, tolgeeProperties.backEndUrl)
        .mapNotNull { originOf(it) }

    if (appOrigin in tolgeeOrigins) {
      throw BadRequestException(Message.APP_MANIFEST_SAME_ORIGIN_AS_TOLGEE, listOf(appOrigin))
    }
  }

  private fun originOf(url: String): String? {
    val uri =
      try {
        URI(url)
      } catch (e: Exception) {
        return null
      }
    val host = uri.host?.lowercase() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    val port = if (uri.port == -1) defaultPortOf(scheme) else uri.port
    return "$scheme://$host:$port"
  }

  private fun defaultPortOf(scheme: String): Int {
    if (scheme == "https") return 443
    return 80
  }

  private fun validateDashboardPages(manifest: AppManifest) {
    requireAbsoluteHttpUrl("baseUrl", manifest.baseUrl)
    rejectTolgeeOrigin(manifest)

    val pages = manifest.modules.projectDashboardPage
    if (pages.isNullOrEmpty()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("manifest must declare at least one project-dashboard-page module"),
      )
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
          throw BadRequestException(
            Message.APP_MANIFEST_INVALID,
            listOf("project-dashboard-page $field must not be blank"),
          )
        }
      }
      if (!seenKeys.add(page.key)) {
        throw BadRequestException(
          Message.APP_MANIFEST_INVALID,
          listOf("duplicate project-dashboard-page key '${page.key}'"),
        )
      }
      requireResolvableHttpUrl("project-dashboard-page '${page.key}' entry", manifest.baseUrl, page.entry)
    }
  }

  /**
   * An emoji or a native icon name passes through as-is; anything containing a slash is an image
   * URL and must resolve to absolute http(s) against the base URL — the browser will load it from
   * the app's host directly, so a broken or non-http value must be refused here.
   */
  private fun resolveIcon(manifest: AppManifest): String? {
    val icon = manifest.icon?.trim()
    if (icon.isNullOrEmpty()) return null
    if (icon.length > MAX_ICON_LENGTH) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("icon exceeds $MAX_ICON_LENGTH characters"),
      )
    }
    if (!icon.contains('/')) return icon
    val resolved =
      try {
        URI(manifest.baseUrl).resolve(icon)
      } catch (e: Exception) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf("invalid icon: ${e.message}"))
      }
    if (!resolved.isAbsoluteHttpUrl()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("icon must resolve to an absolute http(s) URL"),
      )
    }
    return resolved.toString()
  }

  private fun requireAbsoluteHttpUrl(
    field: String,
    value: String,
  ) {
    val uri =
      try {
        URI(value)
      } catch (e: Exception) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf("invalid $field: ${e.message}"))
      }
    if (!uri.isAbsoluteHttpUrl()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("$field must be an absolute http(s) URL"),
      )
    }
  }

  private fun requireResolvableHttpUrl(
    field: String,
    baseUrl: String,
    value: String,
  ) {
    val resolved =
      try {
        URI(baseUrl).resolve(value)
      } catch (e: Exception) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf("invalid $field: ${e.message}"))
      }
    if (!resolved.isAbsoluteHttpUrl()) {
      throw BadRequestException(
        Message.APP_MANIFEST_INVALID,
        listOf("$field must resolve to an absolute http(s) URL"),
      )
    }
  }

  private fun URI.isAbsoluteHttpUrl(): Boolean = scheme?.lowercase() in HTTP_SCHEMES && !host.isNullOrBlank()

  companion object {
    private const val MAX_FIELD_LENGTH = 255
    private const val MAX_ICON_LENGTH = 500
    private val HTTP_SCHEMES = setOf("http", "https")
  }
}
