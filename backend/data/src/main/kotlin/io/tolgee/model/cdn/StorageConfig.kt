package io.tolgee.model.cdn

interface StorageConfig {
  val enabled: Boolean

  val cdnStorageType: CdnStorageType
}
