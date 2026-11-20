package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.contentDelivery.S3ContentStorageConfig

class ContentStoragePublishTestData(
  s3Endpoint: String,
) : BaseTestData() {
  val s3ContentStorage =
    projectBuilder.addContentStorage {
      name = "S3"
      this.s3ContentStorageConfig =
        S3ContentStorageConfig(this).apply {
          bucketName = "bucket"
          accessKey = "accessKey"
          secretKey = "secretKey"
          endpoint = s3Endpoint
          signingRegion = "eu-central-1"
        }
    }

  val contentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = s3ContentStorage.self
      name = "S3"
    }

  init {
    projectBuilder.addKey("key") {
      addTranslation("en", "Hello")
    }
  }
}
