package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty

class HardDeleteKeysRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()
}
