package io.tolgee.component.contentDelivery.cachePurging.awsCloudFront

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.fixtures.removeSlashSuffix
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse
import software.amazon.awssdk.services.cloudfront.model.Paths

class AWSCloudFrontContentDeliveryCachePurging(
  private val config: AWSCloudFrontConfig,
  private val AWSCredentialProvider: AWSCredentialProvider,
) : ContentDeliveryCachePurging {
  override fun purgeForPaths(
    contentDeliveryConfig: ContentDeliveryConfig,
    paths: Set<String>,
  ) {
    val contentPaths = paths.map { "$prefix/${contentDeliveryConfig.slug}/$it" }.toSet()
    val credentialProvider = AWSCredentialProvider.get(config)
    invalidateCloudFrontCache(credentialProvider, config.distributionId, contentPaths)
  }

  private val prefix by lazy {
    var contentRoot = config.contentRoot?.removeSuffix("/") ?: ""
    if (!contentRoot.startsWith("/")) {
      contentRoot = "/$contentRoot"
    }
    contentRoot.removeSlashSuffix()
  }

  private fun createClient(credentialsProvider: StaticCredentialsProvider): CloudFrontClient {
    return CloudFrontClient
      .builder()
      .region(Region.AWS_GLOBAL) // CloudFront is a global service
      .credentialsProvider(credentialsProvider)
      .build()
  }

  private fun buildPaths(pathsToInvalidate: Set<String>): Paths {
    return Paths
      .builder()
      .quantity(pathsToInvalidate.size)
      .items(pathsToInvalidate)
      .build()
  }

  private fun createInvalidationRequest(
    distributionId: String?,
    paths: Paths,
  ): CreateInvalidationRequest {
    return CreateInvalidationRequest
      .builder()
      .distributionId(distributionId)
      .invalidationBatch { batch ->
        batch
          .paths(paths)
          .callerReference(System.currentTimeMillis().toString()) // Unique identifier for request
      }.build()
  }

  private fun createInvalidation(
    client: CloudFrontClient,
    invalidationRequest: CreateInvalidationRequest,
  ): CreateInvalidationResponse {
    return client.createInvalidation(invalidationRequest)
  }

  private fun invalidateCloudFrontCache(
    credentialsProvider: StaticCredentialsProvider,
    distributionId: String?,
    pathsToInvalidate: Set<String>,
  ) {
    val cloudFrontClient = createClient(credentialsProvider)

    val paths = buildPaths(pathsToInvalidate)

    val invalidationRequest = createInvalidationRequest(distributionId, paths)

    createInvalidation(cloudFrontClient, invalidationRequest)

    cloudFrontClient.close()
  }
}
