package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.content-delivery.storage")
class ContentStorageProperties {
  @DocProperty(description = "Configuration of Azure Blob storage")
  var azure: ContentStorageAzureProperties = ContentStorageAzureProperties()

  @DocProperty(description = "Configuration of S3 bucket")
  var s3: ContentStorageS3Properties = ContentStorageS3Properties()

  @DocProperty(
    description =
      "When enabled, project-level content storage endpoints (S3 endpoint, Azure blob endpoint) may target " +
        "otherwise-blocked address ranges — loopback, private/site-local, link-local, IPv6 unique-local, " +
        "multicast and wildcard/any-local addresses. Useful for self-hosted setups with an internal " +
        "S3-compatible store (e.g. MinIO) and for local development.\n" +
        "\n" +
        ":::danger\n" +
        "This removes SSRF protection for content storage targets. Keep it **disabled** on production " +
        "and multi-tenant servers — anyone able to manage content delivery could otherwise reach " +
        "internal services.\n" +
        ":::\n\n",
  )
  var allowLocalAddresses: Boolean = false
}
