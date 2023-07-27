package io.tolgee.model.batch.params

import io.tolgee.model.StandardAuditModel

class ClearTranslationsJobParams : StandardAuditModel() {
  var languageIds: List<Long> = listOf()
}
