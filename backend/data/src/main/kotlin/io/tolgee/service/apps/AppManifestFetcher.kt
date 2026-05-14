package io.tolgee.service.apps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.exceptions.BadRequestException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class AppManifestFetcher(
  private val restTemplate: RestTemplate,
  private val objectMapper: ObjectMapper,
) {
  data class FetchResult(
    val manifest: AppManifest,
    val rawJson: String,
  )

  fun fetch(url: String): FetchResult {
    val rawJson =
      try {
        restTemplate.getForObject(url, String::class.java)
          ?: throw BadRequestException(Message.APP_MANIFEST_FETCH_FAILED)
      } catch (e: RestClientException) {
        throw BadRequestException(Message.APP_MANIFEST_FETCH_FAILED, listOf(e.message ?: ""))
      }

    val manifest =
      try {
        objectMapper.readValue<AppManifest>(rawJson)
      } catch (e: Exception) {
        throw BadRequestException(Message.APP_MANIFEST_INVALID, listOf(e.message ?: ""))
      }

    return FetchResult(manifest = manifest, rawJson = rawJson)
  }
}
