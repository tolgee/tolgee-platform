package io.tolgee.model.batch.params

import io.tolgee.constants.MtServiceType

class PreTranslationByTmJobParams {
  var targetLanguageIds: List<Long> = mutableListOf()

  var service: MtServiceType? = null
}
