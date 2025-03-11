package io.tolgee.component.contentDelivery.cachePurging.awsCloudFront
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest
import software.amazon.awssdk.services.cloudfront.model.Paths
import software.amazon.awssdk.regions.Region
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig


class AWSCloudFrontContentDeliveryCachePurging(
  private val config: AWSCloudFrontConfig,
  private val AWSCredentialProvider: AWSCredentialProvider,
) : ContentDeliveryCachePurging {
  override fun purgeForPaths(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  ) {
    val credentialProvider = AWSCredentialProvider.get(config)
    invalidateCloudFrontCache(credentialProvider, config.distributionId, paths)
  }

  private fun invalidateCloudFrontCache(credentialsProvider: StaticCredentialsProvider, distributionId: String?, pathsToInvalidate: Set<String>) {
    val cloudFrontClient = CloudFrontClient.builder()
        .region(Region.AWS_GLOBAL) // CloudFront is a global service
        .credentialsProvider(credentialsProvider)
        .build()

    val paths = Paths.builder()
        .quantity(pathsToInvalidate.size)
        .items(pathsToInvalidate)
        .build()

    val invalidationRequest = CreateInvalidationRequest.builder()
        .distributionId(distributionId)
        .invalidationBatch { batch ->
            batch.paths(paths)
                .callerReference(System.currentTimeMillis().toString()) // Unique identifier for request
        }
        .build()

    cloudFrontClient.createInvalidation(invalidationRequest)}
}
