package io.tolgee.unit.cachePurging

import com.azure.identity.ClientSecretCredential
import io.tolgee.component.contentDelivery.cachePurging.AzureContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.AzureCredentialProvider
import io.tolgee.model.contentDelivery.AzureFrontDoorConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class AzureContentStorageConfigCachePurgingTest() {
  @Test
  fun `correctly purges`() {
    val config = object : AzureFrontDoorConfig {
      override val clientId: String
        get() = "fake-client-id"
      override val clientSecret: String
        get() = "fake-client-secret"
      override val tenantId: String
        get() = "fake-tenant-id"
      override val contentRoot: String
        get() = "/fake-content-root/"
      override val subscriptionId: String
        get() = "fake-subscription-id"
      override val endpointName: String
        get() = "fake-endpoint-name"
      override val profileName: String
        get() = "fake-profile-name"
      override val resourceGroupName: String
        get() = "fake-resource-group-name"

    }
    val restTemplateMock: RestTemplate = mock()
    val azureCredentialProviderMock: AzureCredentialProvider = mock()
    val purging = AzureContentDeliveryCachePurging(config, restTemplateMock, azureCredentialProviderMock)
    val responseMock: ResponseEntity<*> = Mockito.mock(ResponseEntity::class.java, Mockito.RETURNS_DEEP_STUBS)
    whenever(restTemplateMock.exchange(any<String>(), any<HttpMethod>(), any(), eq(String::class.java))).doAnswer {
      responseMock as ResponseEntity<String>
    }
    whenever(responseMock.statusCode.is2xxSuccessful).thenReturn(true)


    val credentialMck: ClientSecretCredential = Mockito.mock(ClientSecretCredential::class.java, Mockito.RETURNS_DEEP_STUBS)
    whenever(azureCredentialProviderMock.get(config)).thenReturn(credentialMck)
    whenever(credentialMck.getToken(any()).block().token).thenReturn("token")

    purging.purgeForPaths(setOf("fake-path"))

    val invocation = Mockito.mockingDetails(restTemplateMock).invocations.single()
    val url = invocation.arguments[0]
    val headers = (invocation.arguments[2] as HttpEntity<*>).headers
    headers["Authorization"].assert.isEqualTo(listOf("Bearer token"))

    url.assert.isEqualTo(
      "https://management.azure.com/subscriptions/${config.subscriptionId}" +
        "/resourceGroups/${config.resourceGroupName}" +
        "/providers/Microsoft.Cdn/profiles/${config.profileName}" +
        "/afdEndpoints/${config.endpointName}" +
        "/purge?api-version=2023-05-01"
    )
  }
}
