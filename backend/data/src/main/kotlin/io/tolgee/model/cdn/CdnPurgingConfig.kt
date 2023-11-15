package io.tolgee.model.cdn

interface CdnPurgingConfig {
  val enabled: Boolean

  val cdnPurgingType: CdnPurgingType

}
