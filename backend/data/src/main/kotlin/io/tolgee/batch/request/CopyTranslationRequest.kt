package io.tolgee.batch.request

import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

class CopyTranslationRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Min(1)
  var sourceLanguageId: Long = 0

  @Size(min = 1)
  var targetLanguageIds: List<Long> = listOf()
}
