package io.tolgee.batch.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class LabelTranslationsRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var languageIds: List<Long> = listOf()

  @NotEmpty
  var labelIds: List<Long> = listOf()
}
