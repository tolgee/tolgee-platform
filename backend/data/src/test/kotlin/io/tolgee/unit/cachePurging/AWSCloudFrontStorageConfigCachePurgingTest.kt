package io.tolgee.unit.cachePurging

import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCloudFrontContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCredentialProvider
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.CloudFrontClientBuilder
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse

class AWSCloudFrontStorageConfigCachePurgingTest {
  @Test
  fun `correctly purges`() {
    val config =
      object : AWSCloudFrontConfig {
        override val accessKey: String
          get() = "fake-client-id"
        override val secretKey: String
          get() = "fake-client-secret"
        override val distributionId: String
          get() = "fake-distribution-id"
        override val contentRoot: String
          get() = "/fake-content-root/"
      }

    val awsCredentialProviderMock: AWSCredentialProvider = mock()
    val purging = AWSCloudFrontContentDeliveryCachePurging(config, awsCredentialProviderMock)

    val credentialMock: StaticCredentialsProvider = mock()
    whenever(awsCredentialProviderMock.get(config)).thenReturn(credentialMock)

    val cloudFrontClientBuilderMock: CloudFrontClientBuilder = mock()
    val cloudFrontClientMock: CloudFrontClient = mock()

    val createInvalidationResponseMock: CreateInvalidationResponse = mock()

    val staticMock: MockedStatic<CloudFrontClient> = mockStatic(CloudFrontClient::class.java)
    staticMock.use {
      whenever(CloudFrontClient.builder()).thenReturn(cloudFrontClientBuilderMock)
      whenever(cloudFrontClientBuilderMock.region(any<Region>())).thenReturn(cloudFrontClientBuilderMock)
      whenever(
        cloudFrontClientBuilderMock.credentialsProvider(any<StaticCredentialsProvider>()),
      ).thenReturn(cloudFrontClientBuilderMock)
      whenever(cloudFrontClientBuilderMock.build()).thenReturn(cloudFrontClientMock)

      whenever(
        cloudFrontClientMock.createInvalidation(any<CreateInvalidationRequest>()),
      ).thenReturn(createInvalidationResponseMock)

      val contentDeliveryConfig = mock<ContentDeliveryConfig>()
      whenever(contentDeliveryConfig.slug).thenReturn("fake-slug")

      purging.purgeForPaths(
        contentDeliveryConfig,
        setOf("fake-path"),
      )

      verify(awsCredentialProviderMock).get(any())
      verify(cloudFrontClientMock).createInvalidation(any<CreateInvalidationRequest>())
      verify(cloudFrontClientBuilderMock).build()
      verify(cloudFrontClientMock).close()
    }
  }
}
