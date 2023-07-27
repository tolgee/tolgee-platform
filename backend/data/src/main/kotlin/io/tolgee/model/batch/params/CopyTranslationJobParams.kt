package io.tolgee.model.batch.params

import io.tolgee.model.StandardAuditModel

class CopyTranslationJobParams : StandardAuditModel() {
  var sourceLanguageId: Long = 0
  var targetLanguageIds: List<Long> = listOf()
}
