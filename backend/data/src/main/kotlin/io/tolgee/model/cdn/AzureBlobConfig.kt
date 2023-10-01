package io.tolgee.model.cdn

interface AzureBlobConfig : StorageConfig {
  var connectionString: String?
  var containerName: String?

  override val enabled: Boolean
    get() = !connectionString.isNullOrBlank() && !containerName.isNullOrBlank()

  override val cdnStorageType: CdnStorageType
    get() = CdnStorageType.AZURE
}
