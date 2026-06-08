package io.tolgee.api.v2.controllers.configurationProps

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ConfigurationDocumentationProviderTest {
  private val cachePurging: Group by lazy {
    ConfigurationDocumentationProvider()
      .docs
      .group("tolgee")
      .group("contentDelivery")
      .group("cachePurging")
  }

  @Test
  fun `documents the AWS CloudFront cache purging provider`() {
    val awsCloudfront = cachePurging.group("awsCloudfront")
    awsCloudfront.prefix.assert.isEqualTo("tolgee.content-delivery.cache-purging.aws-cloudfront")
    awsCloudfront.children
      .map { it.name }
      .assert
      .contains("accessKey", "secretKey", "distributionId", "contentRoot")
  }

  @Test
  fun `documents the Bunny cache purging provider`() {
    val bunny = cachePurging.group("bunny")
    bunny.prefix.assert.isEqualTo("tolgee.content-delivery.cache-purging.bunny")
    bunny.children
      .map { it.name }
      .assert
      .contains("apiKey", "urlPrefix")
  }

  private fun Group.group(name: String): Group = children.group(name)

  private fun List<DocItem>.group(name: String): Group {
    val item =
      firstOrNull { it.name == name }
        ?: throw AssertionError("No doc item named '$name'; found: ${map { it.name }}")
    return item as? Group
      ?: throw AssertionError("Doc item '$name' is not a group")
  }
}
