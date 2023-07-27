package io.tolgee.model.batch.params

import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.TranslationState

class SetTranslationStateJobParams : StandardAuditModel() {
  var languageIds: List<Long> = listOf()
  var state: TranslationState? = null
}
