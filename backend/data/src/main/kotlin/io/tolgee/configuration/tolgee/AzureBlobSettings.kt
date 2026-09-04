package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.contentDelivery.AzureBlobConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.file-storage.azure")
@DocProperty(
  description =
    "Tolgee supports storing its files in Azure Blob Storage. " +
      "When enabled, Tolgee will store all its files in the configured container rather than in filesystem. " +
      "The container has to exist already.",
  displayName = "Azure Blob Storage",
)
class AzureBlobSettings(
  @DocProperty(
    description =
      "Whether Azure Blob Storage is enabled. If enabled, you need to set all remaining properties below. " +
        "Cannot be enabled together with S3.",
  )
  override var enabled: Boolean = false,
  @DocProperty(description = "Connection string of the Azure Storage account.")
  override var connectionString: String? = null,
  @DocProperty(description = "Name of the container where Tolgee will store its files.")
  override var containerName: String? = null,
) : AzureBlobConfig
