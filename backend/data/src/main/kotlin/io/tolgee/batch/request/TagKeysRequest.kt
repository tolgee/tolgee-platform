package io.tolgee.batch.request

import javax.validation.constraints.NotEmpty

class TagKeysRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @NotEmpty
  var tags: List<String> = listOf()
}
