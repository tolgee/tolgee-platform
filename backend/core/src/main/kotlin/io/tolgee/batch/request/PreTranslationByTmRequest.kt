package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class PreTranslationByTmRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var targetLanguageIds: List<Long> = listOf()
}
