package io.tolgee.dtos

import io.swagger.v3.oas.annotations.media.Schema

interface WithRelatedKeysInOrder {
  @get:Schema(
    description =
      "Keys in the document used as a context for machine translation. " +
        "Keys in the same order as they appear in the document. " +
        "The order is important! We are using it for graph distance calculation. ",
  )
  var relatedKeysInOrder: MutableList<RelatedKeyDto>?
}
