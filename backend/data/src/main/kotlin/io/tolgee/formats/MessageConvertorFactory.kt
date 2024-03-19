package io.tolgee.formats

class MessageConvertorFactory(
  private val message: String,
  private val forceIsPlural: Boolean? = null,
  private val isProjectIcuPlaceholdersEnabled: Boolean = false,
  private val paramConvertorFactory: () -> FromIcuParamConvertor,
) {
  fun create(): BaseIcuMessageConvertor {
    if (!isProjectIcuPlaceholdersEnabled) {
      return BaseIcuMessageConvertor(
        message = message,
        argumentConvertor = NoOpFromIcuParamConvertor(),
        forceIsPlural = forceIsPlural,
      )
    }

    return BaseIcuMessageConvertor(
      message = message,
      argumentConvertor = paramConvertorFactory(),
      forceIsPlural = forceIsPlural,
    )
  }
}
