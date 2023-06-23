package io.tolgee.batch.request

import javax.validation.constraints.NotEmpty

class DeleteKeysRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()
}
