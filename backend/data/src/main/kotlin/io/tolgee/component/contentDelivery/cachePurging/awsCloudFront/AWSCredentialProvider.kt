package io.tolgee.component.contentDelivery.cachePurging.awsCloudFront

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import io.tolgee.model.contentDelivery.AWSCloudFrontConfig

class AWSCredentialProvider {
  fun get(config: AWSCloudFrontConfig): StaticCredentialsProvider {
    return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(
            config.accessKey,
            config.secretKey,
          )
    )
  }
}
