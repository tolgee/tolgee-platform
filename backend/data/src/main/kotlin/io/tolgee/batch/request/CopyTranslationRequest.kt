package io.tolgee.batch.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class CopyTranslationRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Min(1)
  var sourceLanguageId: Long = 0

  @Size(min = 1)
  var targetLanguageIds: List<Long> = listOf()
}
