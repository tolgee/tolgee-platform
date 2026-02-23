package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty

class RestoreKeysRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()
}
