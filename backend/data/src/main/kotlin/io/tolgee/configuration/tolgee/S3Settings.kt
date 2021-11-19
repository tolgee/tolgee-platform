/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage.s3")
class S3Settings(
  var enabled: Boolean = false,
  var accessKey: String? = null,
  var secretKey: String? = null,
  var endpoint: String? = null,
  var signingRegion: String? = null,
  var bucketName: String? = null,
)
