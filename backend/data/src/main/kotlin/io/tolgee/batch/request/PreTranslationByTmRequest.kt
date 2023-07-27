package io.tolgee.batch.request

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

class PreTranslationByTmRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var targetLanguageIds: List<Long> = listOf()
}
