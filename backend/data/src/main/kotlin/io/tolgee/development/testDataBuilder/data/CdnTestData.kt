package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.cdn.AzureCdnConfig
import io.tolgee.model.cdn.S3CdnConfig

class CdnTestData : BaseTestData() {
  val azureCdnStorage = projectBuilder.addCdnStorage {
    this.azureCdnConfig = AzureCdnConfig(this).apply {
      connectionString = "fake"
      containerName = "fake"
    }
  }

  val s3CdnStorage = projectBuilder.addCdnStorage {
    name = "S3"
    this.s3CdnConfig = S3CdnConfig(this).apply {
      bucketName = "fake"
      accessKey = "fake"
      secretKey = "fake"
      endpoint = "fake"
      signingRegion = "fake"
    }
  }

  val defaultServerExporter = projectBuilder.addCdn {
    name = "Default server"
  }

  val azureExporter = projectBuilder.addCdn {
    cdnStorage = azureCdnStorage.self
    name = "Azure"
  }

  val s3Exporter = projectBuilder.addCdn {
    cdnStorage = s3CdnStorage.self
    name = "S3"
  }

  val automation = projectBuilder.addAutomation {
    this.triggers.add(
      AutomationTrigger(this)
        .also { it.type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION }
    )
    this.actions.add(AutomationAction(this).also { it.cdn = defaultServerExporter.self })
  }

  val keyWithTranslation = this.projectBuilder.addKey("key") {
    addTranslation("en", "Hello")
  }
}
