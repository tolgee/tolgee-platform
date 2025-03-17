package io.tolgee.component.contentDelivery.cachePurging.awsCloudFront

import io.tolgee.model.contentDelivery.AWSCloudFrontConfig
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

class AWSCredentialProvider {
  fun get(config: AWSCloudFrontConfig): StaticCredentialsProvider {
    return StaticCredentialsProvider.create(
      AwsBasicCredentials.create(
        config.accessKey,
        config.secretKey,
      ),
    )
  }
}
