package io.tolgee.unit.cachePurging

import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCloudFrontContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.awsCloudFront.AWSCloudFrontContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.azureFrontDoor.AzureContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.azureFrontDoor.AzureContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.bunny.BunnyContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.bunny.BunnyContentDeliveryCachePurgingFactory
import io.tolgee.component.contentDelivery.cachePurging.cloudflare.CloudflareContentDeliveryCachePurging
import io.tolgee.component.contentDelivery.cachePurging.cloudflare.CloudflareContentDeliveryCachePurgingFactory
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate

class ContentDeliveryCachePurgingWiringTest {
  @EnableConfigurationProperties(TolgeeProperties::class)
  @Import(
    ContentDeliveryCachePurgingProvider::class,
    CloudflareContentDeliveryCachePurgingFactory::class,
    BunnyContentDeliveryCachePurgingFactory::class,
    AzureContentDeliveryCachePurgingFactory::class,
    AWSCloudFrontContentDeliveryCachePurgingFactory::class,
  )
  class Config {
    @Bean
    fun restTemplate() = RestTemplate()
  }

  private val contextRunner = ApplicationContextRunner().withUserConfiguration(Config::class.java)

  private val cloudflareProps =
    arrayOf(
      "tolgee.content-delivery.cache-purging.cloudflare.api-key=key",
      "tolgee.content-delivery.cache-purging.cloudflare.url-prefix=https://cdn.example.com",
      "tolgee.content-delivery.cache-purging.cloudflare.zone-id=zone",
    )

  private val bunnyProps =
    arrayOf(
      "tolgee.content-delivery.cache-purging.bunny.api-key=key",
      "tolgee.content-delivery.cache-purging.bunny.url-prefix=https://cdn.example.com",
    )

  private val azureProps =
    arrayOf(
      "tolgee.content-delivery.cache-purging.azure-front-door.client-id=client",
      "tolgee.content-delivery.cache-purging.azure-front-door.client-secret=secret",
      "tolgee.content-delivery.cache-purging.azure-front-door.tenant-id=tenant",
      "tolgee.content-delivery.cache-purging.azure-front-door.subscription-id=subscription",
      "tolgee.content-delivery.cache-purging.azure-front-door.profile-name=profile",
      "tolgee.content-delivery.cache-purging.azure-front-door.endpoint-name=endpoint",
      "tolgee.content-delivery.cache-purging.azure-front-door.resource-group-name=group",
    )

  private val awsProps =
    arrayOf(
      "tolgee.content-delivery.cache-purging.aws-cloudfront.access-key=access",
      "tolgee.content-delivery.cache-purging.aws-cloudfront.secret-key=secret",
      "tolgee.content-delivery.cache-purging.aws-cloudfront.distribution-id=dist",
    )

  private fun assertPurgings(
    vararg properties: String,
    assertion: (List<ContentDeliveryCachePurging>) -> Unit,
  ) {
    contextRunner.withPropertyValues(*properties).run { context ->
      assertion(context.getBean(ContentDeliveryCachePurgingProvider::class.java).purgings)
    }
  }

  @Test
  fun `cloudflare config is wired into a purging`() =
    assertPurgings(*cloudflareProps) {
      it.assert.hasSize(1)
      it.first().assert.isInstanceOf(CloudflareContentDeliveryCachePurging::class.java)
    }

  @Test
  fun `bunny config is wired into a purging`() =
    assertPurgings(*bunnyProps) {
      it.assert.hasSize(1)
      it.first().assert.isInstanceOf(BunnyContentDeliveryCachePurging::class.java)
    }

  @Test
  fun `azure front door config is wired into a purging`() =
    assertPurgings(*azureProps) {
      it.assert.hasSize(1)
      it.first().assert.isInstanceOf(AzureContentDeliveryCachePurging::class.java)
    }

  @Test
  fun `aws cloudfront config is wired into a purging`() =
    assertPurgings(*awsProps) {
      it.assert.hasSize(1)
      it.first().assert.isInstanceOf(AWSCloudFrontContentDeliveryCachePurging::class.java)
    }

  @Test
  fun `all four enabled configs wire into purgings`() =
    assertPurgings(*cloudflareProps, *bunnyProps, *azureProps, *awsProps) {
      it.assert.hasSize(4)
      it.assert.hasAtLeastOneElementOfType(CloudflareContentDeliveryCachePurging::class.java)
      it.assert.hasAtLeastOneElementOfType(BunnyContentDeliveryCachePurging::class.java)
      it.assert.hasAtLeastOneElementOfType(AzureContentDeliveryCachePurging::class.java)
      it.assert.hasAtLeastOneElementOfType(AWSCloudFrontContentDeliveryCachePurging::class.java)
    }

  @Test
  fun `partially configured provider stays disabled`() =
    assertPurgings("tolgee.content-delivery.cache-purging.bunny.api-key=key") {
      it.assert.isEmpty()
    }

  @Test
  fun `no configured providers yields empty purgings without error`() =
    assertPurgings {
      it.assert.isEmpty()
    }
}
