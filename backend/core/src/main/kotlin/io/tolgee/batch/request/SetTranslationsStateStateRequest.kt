package io.tolgee.batch.request

import io.tolgee.model.enums.TranslationState
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class SetTranslationsStateStateRequest {
  @NotEmpty
  var keyIds: List<Long> = listOf()

  @Size(min = 1)
  var languageIds: List<Long> = listOf()

  @NotNull
  var state: TranslationState? = null
}
