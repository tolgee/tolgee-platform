package io.tolgee.formats

class MessageConvertorFactory(
  private val message: String,
  private val forceIsPlural: Boolean? = null,
  private val isProjectIcuPlaceholdersEnabled: Boolean = false,
  private val paramConvertorFactory: () -> FromIcuPlaceholderConvertor,
) {
  fun create(): BaseIcuMessageConvertor {
    return BaseIcuMessageConvertor(
      message = message,
      argumentConvertorFactory = getParamConvertorFactory(),
      forceIsPlural = forceIsPlural,
    )
  }

  private fun getParamConvertorFactory() =
    if (isProjectIcuPlaceholdersEnabled) {
      paramConvertorFactory
    } else {
      { NoOpFromIcuPlaceholderConvertor() }
    }
}
