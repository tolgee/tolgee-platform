package io.tolgee.model.contentDelivery

interface StorageConfig {
  val enabled: Boolean

  val contentStorageType: ContentStorageType
}
