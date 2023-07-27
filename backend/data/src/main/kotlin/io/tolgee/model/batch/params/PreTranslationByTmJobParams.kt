package io.tolgee.model.batch.params

import io.tolgee.constants.MtServiceType
import io.tolgee.model.StandardAuditModel

class PreTranslationByTmJobParams : StandardAuditModel() {

  var targetLanguageIds: List<Long> = mutableListOf()

  var service: MtServiceType? = null
}
