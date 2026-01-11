package io.tolgee.component.contentDelivery.cachePurging.bunny

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.configuration.tolgee.ContentDeliveryBunnyProperties
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder

class BunnyContentDeliveryCachePurging(
  private val config: ContentDeliveryBunnyProperties,
  private val restTemplate: RestTemplate,
) : ContentDeliveryCachePurging {
  override fun purgeForPaths(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  ) {
    executePurgeRequest(contentDeliveryConfig)
  }

  val prefix by lazy {
    config.urlPrefix?.removeSuffix("/") ?: ""
  }

  private fun executePurgeRequest(contentDeliveryConfig: ContentDeliveryConfig) {
    val entity: HttpEntity<String> = getHttpEntity()
    val encodedPath = URLEncoder.encode("$prefix/${contentDeliveryConfig.slug}/*", Charsets.UTF_8)

    val url = "https://api.bunny.net/purge?url=$encodedPath"

    val response =
      restTemplate.exchange(
        url,
        HttpMethod.GET,
        entity,
        String::class.java,
      )

    if (!response.statusCode.is2xxSuccessful) {
      throw IllegalStateException("Purging failed with status code ${response.statusCode}")
    }
  }

  private fun getHttpEntity(): HttpEntity<String> {
    val headers = getHeaders()
    return HttpEntity(null, headers)
  }

  private fun getHeaders(): HttpHeaders {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers.set("AccessKey", "${config.apiKey}")
    return headers
  }
}
