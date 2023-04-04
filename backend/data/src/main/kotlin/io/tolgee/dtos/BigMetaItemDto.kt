package io.tolgee.dtos

import io.tolgee.model.keyBigMeta.SurroundingKey
import javax.validation.constraints.NotEmpty

class BigMetaItemDto {
  var namespace: String? = null

  @field:NotEmpty
  var keyName: String = ""

  @field:NotEmpty
  var location: String = ""

  var contextData: List<SurroundingKey>? = null
}
