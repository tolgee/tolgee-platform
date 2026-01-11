package io.tolgee.unit.cachePurging

import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCloudFrontContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCredentialProvider
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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

  @ParameterizedTest
  @MethodSource("contentRootPathProvider")
  fun `it correctly purges paths with prefix`(
    path: String?,
    expectedPrefix: String,
  ) {
    val cloudFrontClient: CloudFrontClient = mock()
    val credentialsProvider: StaticCredentialsProvider = mock()
    val awsCredentialsProvider: AWSCredentialProvider = mock()

    val cloudFrontConfig =
      object : AWSCloudFrontConfig {
        override val accessKey: String
          get() = "fake-client-id"
        override val secretKey: String
          get() = "fake-client-secret"
        override val distributionId: String
          get() = "fake-distribution-id"
        override val contentRoot: String?
          get() = path
      }

    whenever(awsCredentialsProvider.get(cloudFrontConfig)).thenReturn(credentialsProvider)

    val purger = AWSCloudFrontContentDeliveryCachePurging(cloudFrontConfig, awsCredentialsProvider)

    mockStatic(CloudFrontClient::class.java).use {
      val builder = mock<CloudFrontClientBuilder>()

      `when`(CloudFrontClient.builder()).thenReturn(builder)
      `when`(builder.region(Region.AWS_GLOBAL)).thenReturn(builder)
      `when`(builder.credentialsProvider(credentialsProvider)).thenReturn(builder)
      `when`(builder.build()).thenReturn(cloudFrontClient)

      `when`(cloudFrontClient.createInvalidation(any<CreateInvalidationRequest>())).thenReturn(
        CreateInvalidationResponse.builder().build(),
      )

      val contentDeliveryConfig = mock<ContentDeliveryConfig>()
      `when`(contentDeliveryConfig.slug).thenReturn("f1a7d82c4b3597e1d94c2efb3896a3d5")

      purger.purgeForPaths(
        contentDeliveryConfig = contentDeliveryConfig,
        paths = setOf("common/en.json", "common/cs.json"),
      )

      val captor = argumentCaptor<CreateInvalidationRequest>()
      verify(cloudFrontClient).createInvalidation(captor.capture())

      val request = captor.firstValue
      val paths = request.invalidationBatch().paths().items()

      paths.assert.containsExactly(
        "$expectedPrefix/f1a7d82c4b3597e1d94c2efb3896a3d5/common/en.json",
        "$expectedPrefix/f1a7d82c4b3597e1d94c2efb3896a3d5/common/cs.json",
      )

      verify(cloudFrontClient).close()
    }
  }

  companion object {
    @JvmStatic
    fun contentRootPathProvider() =
      listOf(
        Arguments.of("", ""),
        Arguments.of(null, ""),
        Arguments.of("/", ""),
        Arguments.of("/fake-content-root/", "/fake-content-root"),
        Arguments.of("fake-content-root/", "/fake-content-root"),
        Arguments.of("/fake-content-root", "/fake-content-root"),
        Arguments.of("fake-content-root", "/fake-content-root"),
      )
  }
}
