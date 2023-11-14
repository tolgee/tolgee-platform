package io.tolgee.model.cdn

import io.swagger.v3.oas.annotations.media.Schema

interface AzureBlobConfig : StorageConfig {
  var connectionString: String?
  var containerName: String?

  @get:Schema(hidden = true)
  override val enabled: Boolean
    get() = !connectionString.isNullOrBlank() && !containerName.isNullOrBlank()

  @get:Schema(hidden = true)
  override val cdnStorageType: CdnStorageType
    get() = CdnStorageType.AZURE
}
