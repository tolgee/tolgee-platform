/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.S3Config
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage.s3")
@DocProperty(
  description =
    "Tolgee supports storing its files on an S3-compatible storage server. " +
      "When enabled, Tolgee will store all its files on the S3 server rather than in filesystem.",
  displayName = "S3",
)
class S3Settings(
  @DocProperty(description = "Whether S3 is enabled. If enabled, you need to set all remaining properties below.")
  override var enabled: Boolean = false,
  @DocProperty(
    description =
      "Access key for the S3 server. (optional if you are authenticating " +
        "with a different method, like STS Web Identity)",
  )
  override var accessKey: String? = null,
  @DocProperty(
    description =
      "Secret key for the access key. (optional if you are authenticating " +
        "with a different method, like STS Web Identity)",
  )
  override var secretKey: String? = null,
  @DocProperty(
    description =
      "Has to be set to a service endpoint: " +
        "https://docs.aws.amazon.com/general/latest/gr/s3.html",
  )
  override var endpoint: String? = null,
  @DocProperty(description = "Has to be set to a signing region: https://docs.aws.amazon.com/general/latest/gr/s3.html")
  override var signingRegion: String? = null,
  @DocProperty(description = "Name of the bucket where Tolgee will store its files.")
  override var bucketName: String? = null,
  @DocProperty(description = "Optional subfolder structure within s3 bucket to which content will be stored")
  override var path: String? = null,
) : S3Config
