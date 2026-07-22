package io.tolgee.model.contentDelivery

import io.swagger.v3.oas.annotations.media.Schema

interface StorageConfig {
  // Derived server-side, not a client input. Kept out of the OpenAPI schema so it isn't reported as
  // a required request property (Hibernate/springdoc under Boot 4 now reads the Kotlin non-null type).
  @get:Schema(hidden = true)
  val enabled: Boolean

  @get:Schema(hidden = true)
  val contentStorageType: ContentStorageType
}
