package io.tolgee.hateoas.ee.contentStorage

class S3ContentStorageConfigModel(
  var bucketName: String = "",
  var endpoint: String = "",
  var signingRegion: String = "",
  var path: String = "",
)
