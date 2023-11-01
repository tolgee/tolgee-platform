package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.cdn.AzureCdnConfig
import io.tolgee.model.cdn.S3CdnConfig

class CdnExporterTestData : BaseTestData() {
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

  val defaultServerExporter = projectBuilder.addCdnExporter {
    name = "Default server"
  }

  val azureExporter = projectBuilder.addCdnExporter {
    cdnStorage = azureCdnStorage.self
    name = "Azure"
  }

  val s3Exporter = projectBuilder.addCdnExporter {
    cdnStorage = s3CdnStorage.self
    name = "S3"
  }

  val keyWithTranslation = this.projectBuilder.addKey("key") {
    addTranslation("en", "Hello")
  }
}
