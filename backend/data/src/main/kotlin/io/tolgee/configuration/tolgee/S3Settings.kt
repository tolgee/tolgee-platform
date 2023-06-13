/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage.s3")
@DocProperty(
  description = "Tolgee supports storing its files on an S3-compatible storage server. " +
    "When enabled, Tolgee will store all its files on the S3 server rather than in filesystem.",
  displayName = "S3"
)
class S3Settings(
  @DocProperty(description = "Whether S3 is enabled. If enabled, you need to set all remaining properties below.")
  var enabled: Boolean = false,

  @DocProperty(description = "Access key for the S3 server. (optional if you are authenticating " +
    "with a different method, like STS Web Identity)")
  var accessKey: String? = null,

  @DocProperty(description = "Secret key for the access key. (optional if you are authenticating " +
    "with a different method, like STS Web Identity)")
  var secretKey: String? = null,

  @DocProperty(
    description = "Has to be set to a service endpoint: " +
      "https://docs.aws.amazon.com/general/latest/gr/s3.html"
  )
  var endpoint: String? = null,

  @DocProperty(description = "Has to be set to a signing region: https://docs.aws.amazon.com/general/latest/gr/s3.html")
  var signingRegion: String? = null,

  @DocProperty(description = "Name of the bucket where Tolgee will store its files.")
  var bucketName: String? = null,
)
