package io.tolgee.dtos.queryResults.keyDisabledLanguages

class KeyDisabledLanguagesView(
  val id: Long,
  val name: String,
  val namespace: String?,
  val disabledLanguages: List<KeyDisabledLanguageModel>,
) {
  class KeyDisabledLanguageModel(
    val id: Long,
    val tag: String,
  )
}
