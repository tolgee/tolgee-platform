/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage.s3")
class S3Settings(
  var enabled: Boolean = false,
  var accessKey: String? = null,
  var secretKey: String? = null,
  @Schema(description = "Has to be set to a service endpoint: https://docs.aws.amazon.com/general/latest/gr/s3.html")
  var endpoint: String? = null,
  var signingRegion: String? = null,
  var bucketName: String? = null,
)
