package io.tolgee.model.batch.params

import io.tolgee.model.enums.TranslationState

class SetTranslationStateJobParams {
  var languageIds: List<Long> = listOf()
  var state: TranslationState? = null
}
