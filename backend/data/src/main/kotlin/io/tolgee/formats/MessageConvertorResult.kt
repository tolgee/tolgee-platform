package io.tolgee.formats

data class MessageConvertorResult(
  val message: String?,
  val pluralArgName: String?,
  val customValuesModifier: (
    (
      customValues: MutableMap<String, Any?>,
      memory: MutableMap<String, Any?>,
    ) -> Unit
  )? = null,
)
