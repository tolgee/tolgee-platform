package io.tolgee.model.enums

enum class AssignableTranslationState(
  val translationState: TranslationState,
) {
  TRANSLATED(TranslationState.TRANSLATED),
  REVIEWED(TranslationState.REVIEWED),
}
