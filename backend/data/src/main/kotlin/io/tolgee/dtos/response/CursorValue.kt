package io.tolgee.dtos.response

import org.springframework.data.domain.Sort

data class CursorValue(
  val direction: Sort.Direction,
  val value: String?,
)
