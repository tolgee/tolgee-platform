package io.tolgee.service.machineTranslation

import io.tolgee.constants.MtServiceType

data class MachineTranslationParams(
  var keyId: Long? = null,
  var baseTranslationText: String? = null,
  var targetLanguageId: Long? = null,
  var targetLanguageIds: List<Long> = listOf(),
  var desiredServices: Set<MtServiceType>? = null,
  var usePrimaryService: Boolean = false,
  var useAllEnabledServices: Boolean = false,
  var isBatch: Boolean = false,
) {
  fun allTargetLanguages(): Set<Long> {
    return (targetLanguageIds + listOfNotNull(targetLanguageId)).toSet()
  }
}
