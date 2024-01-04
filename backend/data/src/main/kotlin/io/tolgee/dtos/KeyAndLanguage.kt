package io.tolgee.dtos

import io.tolgee.model.Language
import io.tolgee.model.key.Key

data class KeyAndLanguage(
  val key: Key,
  val language: Language,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KeyAndLanguage

    if (key.id != other.key.id) return false
    if (language.id != other.language.id) return false

    return true
  }

  override fun hashCode(): Int {
    var result = key.id.hashCode()
    result = 31 * result + language.id.hashCode()
    return result
  }
}
