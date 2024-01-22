package io.tolgee.ee.api.v2.hateoas.contentStorage

class S3ContentStorageConfigModel(
  var bucketName: String = "",
  var endpoint: String = "",
  var signingRegion: String = "",
)
