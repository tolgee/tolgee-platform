package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class ClearTranslationsRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var languageIds: List<Long> = listOf()
}
