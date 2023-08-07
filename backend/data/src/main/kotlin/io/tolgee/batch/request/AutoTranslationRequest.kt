package io.tolgee.batch.request

import io.tolgee.batch.data.BatchTranslationTargetItem

class AutoTranslationRequest {
  var target: List<BatchTranslationTargetItem> = listOf()
  var useTranslationMemory = false
  var useMachineTranslation = false
}
