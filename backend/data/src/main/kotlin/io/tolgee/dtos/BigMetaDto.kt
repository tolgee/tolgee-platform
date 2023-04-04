package io.tolgee.dtos

import javax.validation.constraints.Size

class BigMetaDto {
  @field:Size(max = 100)
  val items: MutableList<BigMetaItemDto> = mutableListOf()
}
