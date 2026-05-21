package io.tolgee.service.dataImport

import io.tolgee.formats.convertToIcuPlurals
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.translation.TranslationService

class PluralizationHandler(
  private val importDataManager: ImportDataManager,
  private val storedDataImporter: StoredDataImporter,
  private val translationService: TranslationService,
) {
  /**
   * Map (keyId -> pluralArgName)
   */
  private val existingKeysToMigrate = mutableMapOf<Long, String>()
  private val ignoreTranslationsForMigration: MutableList<Long> = mutableListOf()
  val keysToSave = mutableListOf<Key>()

  fun handlePluralization() {
    val byKey =
      storedDataImporter.translationsToSave
        .groupBy { it.key.namespace?.name to it.key.name }
    byKey.forEach { (keyId, translations) ->
      handleKeyPluralization(keyId, translations)
    }

    migrateExistingKeys()
  }

  private fun migrateExistingKeys() {
    if (existingKeysToMigrate.isEmpty()) {
      return
    }

    translationService.onKeyIsPluralChanged(existingKeysToMigrate, true, ignoreTranslationsForMigration)
  }

  private fun handleKeyPluralization(
    keyId: Pair<String?, String>,
    translations: List<Translation>,
  ) {
    val existingKey = importDataManager.existingKeys[keyId]

    if (existingKey?.isPlural == true) {
      migrateNewTranslationsToPlurals(translations, existingKey.pluralArgName)
      return
    }

    val sourceArgName = storedDataImporter.pluralFlipKeys[keyId] ?: return

    val newKey = translations.first().key
    newKey.isPlural = true
    val pluralArgName = migrateNewTranslationsToPlurals(translations, sourceArgName)
    newKey.pluralArgName = pluralArgName
    keysToSave.add(newKey)

    // For an existing non-plural key, migrate its DB-resident
    // other-language translations to plural form too.
    if (existingKey != null) {
      existingKeysToMigrate[existingKey.id] = pluralArgName
      ignoreTranslationsForMigration.addAll(translations.map { it.id })
    }
  }

  private fun migrateNewTranslationsToPlurals(
    translations: List<Translation>,
    pluralArgName: String?,
  ): String {
    val map = translations.associateWith { it.text }
    val conversionResult = map.convertToIcuPlurals(pluralArgName)
    conversionResult.convertedStrings.forEach { (translation, newText) ->
      translation.text = newText
    }
    return conversionResult.argName
  }
}
