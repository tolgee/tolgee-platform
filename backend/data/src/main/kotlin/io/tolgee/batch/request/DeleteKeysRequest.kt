package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty

class DeleteKeysRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()
}
