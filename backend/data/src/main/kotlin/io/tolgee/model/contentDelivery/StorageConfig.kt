package io.tolgee.model.contentDelivery

import io.swagger.v3.oas.annotations.media.Schema

interface StorageConfig {
  @get:Schema(hidden = true)
  val enabled: Boolean

  @get:Schema(hidden = true)
  val contentStorageType: ContentStorageType
}
