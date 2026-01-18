package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty

class NoOpRequest {
  @NotEmpty
  var itemIds: List<Long> = listOf()
}
