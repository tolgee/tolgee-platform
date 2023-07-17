package io.tolgee.batch.request

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

class ClearTranslationsRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var languageIds: List<Long> = listOf()
}
