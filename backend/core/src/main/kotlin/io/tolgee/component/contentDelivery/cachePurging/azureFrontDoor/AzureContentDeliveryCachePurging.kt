package io.tolgee.component.contentDelivery.cachePurging.azureFrontDoor

import com.azure.core.credential.TokenRequestContext
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.model.contentDelivery.AzureFrontDoorConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

class AzureContentDeliveryCachePurging(
  private val config: AzureFrontDoorConfig,
  private val restTemplate: RestTemplate,
  private val azureCredentialProvider: AzureCredentialProvider,
) : ContentDeliveryCachePurging {
  override fun purgeForPaths(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  ) {
    val token = getAccessToken()
    purgeWithToken(contentDeliveryConfig, token)
  }

  private fun purgeWithToken(
    contentDeliveryConfig: ContentDeliveryConfig,
    token: String,
  ) {
    val contentRoot = config.contentRoot?.removeSuffix("/") ?: ""
    val body = mapOf("contentPaths" to listOf("$contentRoot/${contentDeliveryConfig.slug}/*"))
    executePurgeRequest(token, body, config)
  }

  private fun executePurgeRequest(
    token: String,
    body: Map<String, List<String>>,
    config: AzureFrontDoorConfig,
  ) {
    val entity: HttpEntity<String> = getHttpEntity(token, body)

    val url =
      "https://management.azure.com/subscriptions/${config.subscriptionId}" +
        "/resourceGroups/${config.resourceGroupName}" +
        "/providers/Microsoft.Cdn/profiles/${config.profileName}" +
        "/afdEndpoints/${config.endpointName}" +
        "/purge?api-version=2023-05-01"

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

  private fun getHttpEntity(
    token: String,
    body: Map<String, List<String>>,
  ): HttpEntity<String> {
    val headers = getHeaders(token)
    val jsonBody = jacksonObjectMapper().writeValueAsString(body)
    return HttpEntity(jsonBody, headers)
  }

  private fun getHeaders(token: String): HttpHeaders {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers.set("Authorization", "Bearer $token")
    return headers
  }

  private fun getAccessToken(): String {
    val credential = azureCredentialProvider.get(config)

    val context =
      TokenRequestContext()
        .addScopes("https://management.azure.com/.default")

    return credential.getToken(context).block()?.token ?: throw IllegalStateException("No token")
  }
}
