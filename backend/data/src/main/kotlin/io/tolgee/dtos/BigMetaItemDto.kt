package io.tolgee.dtos

import io.tolgee.model.keyBigMeta.BigMetaType
import javax.validation.constraints.NotEmpty

class BigMetaItemDto {
  var namespace: String? = null

  @field:NotEmpty
  var keyName: String = ""

  @field:NotEmpty
  var location: String = ""

  var type: BigMetaType = BigMetaType.SCRAPE

  var contextData: Any? = null
}
