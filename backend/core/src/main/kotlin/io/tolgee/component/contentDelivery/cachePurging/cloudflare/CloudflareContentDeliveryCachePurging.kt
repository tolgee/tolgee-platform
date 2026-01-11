package io.tolgee.component.contentDelivery.cachePurging.cloudflare

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.configuration.tolgee.ContentDeliveryCloudflareProperties
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

class CloudflareContentDeliveryCachePurging(
  private val config: ContentDeliveryCloudflareProperties,
  private val restTemplate: RestTemplate,
) : ContentDeliveryCachePurging {
  override fun purgeForPaths(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  ) {
    val bodies = getChunkedBody(paths, contentDeliveryConfig)
    executePurgeRequest(bodies)
  }

  private fun executePurgeRequest(bodies: List<Map<String, List<Map<String, Any>>>>) {
    bodies.forEach { body ->
      val entity: HttpEntity<String> = getHttpEntity(body)

      val url = "https://api.cloudflare.com/client/v4/zones/${config.zoneId}/purge_cache"

      val response =
        restTemplate.exchange(
          url,
          HttpMethod.POST,
          entity,
          String::class.java,
        )

      if (!response.statusCode.is2xxSuccessful) {
        throw IllegalStateException("Purging failed with status code ${response.statusCode}")
      }
    }
  }

  private fun getChunkedBody(
    paths: Set<String>,
    contentDeliveryConfig: ContentDeliveryConfig,
  ): List<Map<String, List<Map<String, Any>>>> {
    return paths
      .flatMap {
        getFileItems(contentDeliveryConfig, it)
      }.chunked(config.maxFilesPerRequest)
      .map { fileItems ->
        mapOf("files" to fileItems)
      }
  }

  private fun getFileItems(
    contentDeliveryConfig: ContentDeliveryConfig,
    path: String,
  ): List<Map<String, Any>> {
    return origins.map { origin ->
      val map =
        mutableMapOf<String, Any>(
          "url" to "$prefix/${contentDeliveryConfig.slug}/$path",
        )

      if (origin != null) {
        map["headers"] =
          mapOf(
            "Origin" to origin,
          )
      }

      map
    }
  }

  val origins by lazy {
    config.origins?.split(",") ?: listOf(null)
  }

  val prefix by lazy {
    config.urlPrefix?.removeSuffix("/") ?: ""
  }

  private fun getHttpEntity(body: Any): HttpEntity<String> {
    val headers = getHeaders()
    val jsonBody = jacksonObjectMapper().writeValueAsString(body)
    return HttpEntity(jsonBody, headers)
  }

  private fun getHeaders(): HttpHeaders {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers.set("Authorization", "Bearer ${config.apiKey}")
    return headers
  }
}
