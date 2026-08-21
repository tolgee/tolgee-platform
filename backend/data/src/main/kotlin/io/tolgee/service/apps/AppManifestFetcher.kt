package io.tolgee.service.apps

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppManifestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.Scope
import io.tolgee.util.UrlSecurity
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class AppManifestFetcher(
  private val appManifestHttpClient: AppManifestHttpClient,
  private val objectMapper: ObjectMapper,
  private val urlSecurity: UrlSecurity,
  private val appsProperties: AppsProperties,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun fetch(url: String): FetchResult {
    urlSecurity.validateUrl(url, allowLocalAddresses = appsProperties.allowLocalAddresses)
    val rawJson = appManifestHttpClient.fetchBody(url)
    val manifest = parseManifest(rawJson)
    val iconResolver = AppIconResolver(manifest.icon, manifest.baseUrl)
    AppManifestValidator(manifest, tolgeeProperties, iconResolver).validate()
    return FetchResult(
      manifest = manifest,
      rawJson = rawJson,
      scopes = Scope.parse(manifest.scopes),
      icon = iconResolver.resolve(),
    )
  }

  private fun parseManifest(rawJson: String): AppManifestDto {
    return try {
      objectMapper.readValue<AppManifestDto>(rawJson)
    } catch (e: Exception) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf(e.message ?: ""), e)
    }
  }

  data class FetchResult(
    val manifest: AppManifestDto,
    val rawJson: String,
    val scopes: Set<Scope>,
    /** The manifest's icon with a relative image URL already resolved against the base URL. */
    val icon: String? = null,
  )
}
