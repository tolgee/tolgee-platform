package io.tolgee.ee.data

import jakarta.validation.constraints.NotEmpty

data class AiPlaygroundResultRequest(
  @field:NotEmpty
  var keys: List<Long>? = null,

  @field:NotEmpty
  var languages: List<Long>? = null,
)
