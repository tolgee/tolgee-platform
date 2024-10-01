package io.tolgee.formats

class MessageConvertorFactory(
  private val message: String,
  private val forceIsPlural: Boolean,
  private val isProjectIcuPlaceholdersEnabled: Boolean = false,
  private val paramConvertorFactory: () -> FromIcuPlaceholderConvertor,
) {
  fun create(): BaseIcuMessageConvertor {
    return BaseIcuMessageConvertor(
      message = message,
      argumentConvertorFactory = getParamConvertorFactory(),
      forceIsPlural = forceIsPlural,
      keepEscaping = keepEscaping,
    )
  }

  private val keepEscaping by lazy {
    if (forceIsPlural) {
      return@lazy false
    }

    return@lazy !isProjectIcuPlaceholdersEnabled
  }

  private fun getParamConvertorFactory() =
    if (isProjectIcuPlaceholdersEnabled) {
      paramConvertorFactory
    } else {
      { NoOpFromIcuPlaceholderConvertor() }
    }
}
