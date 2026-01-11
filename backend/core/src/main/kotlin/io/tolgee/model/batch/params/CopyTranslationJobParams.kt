package io.tolgee.model.batch.params

class CopyTranslationJobParams {
  var sourceLanguageId: Long = 0
  var targetLanguageIds: List<Long> = listOf()
}
