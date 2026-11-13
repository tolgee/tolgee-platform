package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.formats.ExportFormat
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

  val zipEnabledContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      contentStorage = s3ContentStorage.self
      name = "Zip Enabled"
      zip = true
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
      addTag("release")
      addTag("wip")
    }

  val hiddenParamsContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      name = "Hidden params"
      format = ExportFormat.JSON
      structureDelimiter = '/'
      filterKeyPrefix = "release."
      filterKeyIdNot = listOf(999999L)
      fileStructureTemplate = "{languageTag}/messages.{extension}"
    }

  val legacyTagContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      name = "Legacy tag"
      filterTag = "legacy"
      filterTagIn = listOf("in-list")
    }

  init {
    root.rawUpdateAfterSave(
      "update content_delivery_config set filter_key_id = cast(:filterKeyId as jsonb) where id = :id",
    ) {
      mapOf(
        "filterKeyId" to "[${keyWithTranslation.self.id}]",
        "id" to hiddenParamsContentDeliveryConfig.self.id,
      )
    }
  }

  // A separate project (owned by an unrelated organization) with its own storage.
  // Used to verify that access to a storage by id from another project is rejected.
  val unrelatedOrg =
    root.addOrganization {
      name = "Unrelated org"
    }

  val unrelatedProject: ProjectBuilder =
    root
      .addProject(organizationOwner = unrelatedOrg.self) {
        name = "Unrelated project"
      }.build {
        addEnglish()
      }

  val unrelatedAzureContentStorage =
    unrelatedProject.addContentStorage {
      name = "Unrelated Azure"
      this.azureContentStorageConfig =
        AzureContentStorageConfig(this).apply {
          connectionString = "unrelatedConnectionString"
          containerName = "unrelatedContainerName"
        }
    }
}
