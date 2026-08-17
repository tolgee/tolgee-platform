package io.tolgee.development.testDataBuilder.data

import io.tolgee.formats.ExportFormat
import io.tolgee.model.contentDelivery.AzureContentStorageConfig

class ContentDeliveryContentTypeTestData :
  BaseTestData(
    userName = "content_type_username",
    projectName = "content_type_project",
  ) {
  val azureContentStorage =
    projectBuilder.addContentStorage {
      this.azureContentStorageConfig =
        AzureContentStorageConfig(this).apply {
          connectionString = "fake"
          containerName = "fake"
        }
    }

  val appleContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = azureContentStorage.self
      name = "Apple"
      format = ExportFormat.APPLE_STRINGS_STRINGSDICT
    }

  val unrecoverableExtensionContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = azureContentStorage.self
      name = "Apple with unrecoverable extension"
      format = ExportFormat.APPLE_STRINGS_STRINGSDICT
      fileStructureTemplate = "{languageTag}{extension}"
    }

  val keyForcingStringsFile =
    projectBuilder.addKey("key") {
      addTranslation("en", "Hello")
    }

  val keyForcingStringsdictFile =
    projectBuilder
      .addKey {
        name = "plural key"
        isPlural = true
        pluralArgName = "count"
      }.build {
        addTranslation("en", "{count, plural, one {I am one} other {I am other}}")
      }
}
