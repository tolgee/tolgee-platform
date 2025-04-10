package io.tolgee.formats.apple.`in`

import io.tolgee.formats.CollisionHandler
import io.tolgee.model.dataImport.ImportTranslation
import org.springframework.stereotype.Component

@Component
class AppleCollisionHandler : CollisionHandler {
  /**
   * Takes the colliding translations and returns the ones to be deleted
   */
  override fun handle(importTranslations: List<ImportTranslation>): List<ImportTranslation>? {
    val strings =
      importTranslations
        .filter {
          it.key.file.name
            ?.matches(STRINGS_FILE_REGEX) == true
        }

    val stringsDict =
      importTranslations
        .filter {
          it.key.file.name
            ?.matches(STRINGSDICT_FILE_REGEX) == true
        }

    if (strings.isEmpty() || stringsDict.isEmpty()) {
      return null
    }

    if (importTranslations.size - strings.size == 1) {
      return strings
    }

    return null
  }

  companion object {
    val STRINGS_FILE_REGEX by lazy { ".*\\.strings".toRegex() }
    val STRINGSDICT_FILE_REGEX by lazy { ".*\\.stringsdict".toRegex() }
  }
}
