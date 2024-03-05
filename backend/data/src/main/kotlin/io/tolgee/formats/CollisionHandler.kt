package io.tolgee.formats

import io.tolgee.model.dataImport.ImportTranslation

interface CollisionHandler {
  /**
   * Takes the colliding translations and returns the ones to be deleted
   */
  fun handle(importTranslations: List<ImportTranslation>): List<ImportTranslation>?
}
