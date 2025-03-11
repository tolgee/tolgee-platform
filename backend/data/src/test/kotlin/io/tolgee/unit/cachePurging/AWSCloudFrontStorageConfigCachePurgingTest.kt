package io.tolgee.unit.cachePurging
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.cloudfront.model.CloudFrontException
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCloudFrontContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCredentialProvider
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import io.tolgee.testing.assert
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


class AWSCloudflareContentStorageConfigCachePurgingTest() {
  @Test
  fun `correctly purges`() {
    val config =
      object : AWSCloudFrontConfig {
        override val accessKey: String
          get() = "fake-client-id"
        override val secretKey: String
          get() = "fake-client-secret"
        override val distributionId: String
          get() = "fake-distrution-id/"
        override val contentRoot: String
          get() = "/fake-content-root/"
      }
    val awsCredentialProviderMock: AWSCredentialProvider = mock()
    val purging = AWSCloudFrontContentDeliveryCachePurging(config, awsCredentialProviderMock)

    val credentialMck: StaticCredentialsProvider =
      Mockito.mock(StaticCredentialsProvider::class.java, Mockito.RETURNS_DEEP_STUBS)
    whenever(awsCredentialProviderMock.get(config)).thenReturn(credentialMck)

    val contentDeliveryConfig = mock<ContentDeliveryConfig>()
    whenever(contentDeliveryConfig.slug).thenReturn("fake-slug")

    val exception = assertThrows<CloudFrontException> {
            purging.purgeForPaths(contentDeliveryConfig, setOf("fake-path"))
    }
    
    exception.message?.startsWith("Missing Authentication Token (Service: CloudFront, Status Code: 403, Request ID:").assert.isTrue

    
  }
}
