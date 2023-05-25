/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage.s3")
@DocProperty(
  description = "Tolgee supports storing its files on an S3-compatible storage server. " +
    "When enabled, Tolgee will store all its files on the S3 server rather than in filesystem."
)
class S3Settings(
  var enabled: Boolean = false,
  var accessKey: String? = null,
  var secretKey: String? = null,
  @DocProperty(description = "Has to be set to a service endpoint: https://docs.aws.amazon.com/general/latest/gr/s3.html")
  var endpoint: String? = null,
  var signingRegion: String? = null,
  var bucketName: String? = null,
)
