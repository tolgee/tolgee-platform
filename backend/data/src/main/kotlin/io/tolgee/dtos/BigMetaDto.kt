package io.tolgee.dtos

import io.swagger.v3.oas.annotations.media.Schema

class BigMetaDto {
  @field:Schema(
    description = "List of keys, visible, in order as they appear in the document. " +
      "The order is important! We are using it for graph distance calculation. "
  )
  var relatedKeysInOrder: MutableList<RelatedKeyDto> = mutableListOf()
}
