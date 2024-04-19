package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.contentDelivery.AzureContentStorageConfig
import io.tolgee.model.contentDelivery.S3ContentStorageConfig

class ContentDeliveryConfigTestData : BaseTestData() {
  val azureContentStorage =
    projectBuilder.addContentStorage {
      this.azureContentStorageConfig =
        AzureContentStorageConfig(this).apply {
          connectionString = "fake"
          containerName = "fake"
        }
    }

  val s3ContentStorage =
    projectBuilder.addContentStorage {
      name = "S3"
      this.s3ContentStorageConfig =
        S3ContentStorageConfig(this).apply {
          bucketName = "fake"
          accessKey = "fake"
          secretKey = "fake"
          endpoint = "fake"
          signingRegion = "fake"
        }
    }

  val defaultServerContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      name = "Default server"
    }

  val azureContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = azureContentStorage.self
      name = "Azure"
    }

  val s3ContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = s3ContentStorage.self
      name = "S3"
    }

  val s3ContentDeliveryConfigWithCustomSlug =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = s3ContentStorage.self
      name = "Custom Slug"
      slug = "my-slug"
      customSlug = true
    }

  val automation =
    projectBuilder.addAutomation {
      this.triggers.add(
        AutomationTrigger(this)
          .also { it.type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION },
      )
      this.actions.add(
        AutomationAction(this).also { it.contentDeliveryConfig = defaultServerContentDeliveryConfig.self },
      )
    }

  val keyWithTranslation =
    this.projectBuilder.addKey("key") {
      addTranslation("en", "Hello")
    }
}
