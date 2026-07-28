package io.tolgee.model.enums

/**
 * Ordinal-persisted by [io.tolgee.model.translation.Translation.state] and
 * [io.tolgee.model.branching.snapshot.TranslationSnapshot.state] — do not reorder.
 */
enum class TranslationState {
  UNTRANSLATED,
  TRANSLATED,
  REVIEWED,
  DISABLED,
}
