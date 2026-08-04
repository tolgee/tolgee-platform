package io.tolgee.service.apps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import io.tolgee.util.UrlSecurity
import org.springframework.stereotype.Component
import java.net.URI

@Component
class AppManifestFetcher(
  private val appManifestHttpClient: AppManifestHttpClient,
  private val objectMapper: ObjectMapper,
  private val urlSecurity: UrlSecurity,
  private val appsProperties: AppsProperties,
) {
  data class FetchResult(
    val manifest: AppManifest,
    val rawJson: String,
    val scopes: Set<Scope>,
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

  private fun validateDashboardPages(manifest: AppManifest) {
    requireAbsoluteHttpUrl("baseUrl", manifest.baseUrl)

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
    private val HTTP_SCHEMES = setOf("http", "https")
  }
}
