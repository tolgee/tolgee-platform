package io.tolgee.batch.request

import io.tolgee.model.enums.TranslationState
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

class SetTranslationsStateStateRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var languageIds: List<Long> = listOf()

  @NotNull
  var state: TranslationState? = null
}
