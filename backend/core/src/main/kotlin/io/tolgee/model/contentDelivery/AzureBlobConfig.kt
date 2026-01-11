package io.tolgee.model.contentDelivery

import io.swagger.v3.oas.annotations.media.Schema

interface AzureBlobConfig : StorageConfig {
  var connectionString: String?
  var containerName: String?

  @get:Schema(hidden = true)
  override val enabled: Boolean
    get() = !connectionString.isNullOrBlank() && !containerName.isNullOrBlank()

  @get:Schema(hidden = true)
  override val contentStorageType: ContentStorageType
    get() = ContentStorageType.AZURE
}
