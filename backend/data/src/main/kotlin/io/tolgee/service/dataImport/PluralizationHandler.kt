package io.tolgee.service.dataImport

import io.tolgee.formats.convertToIcuPlurals
import io.tolgee.model.dataImport.ImportTranslation
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
        .groupBy {
          it.second.key.namespace
            ?.name to it.second.key.name
        }
    byKey.forEach {
      handleKeyPluralization(it)
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
    data: Map.Entry<Pair<String?, String>, List<Pair<ImportTranslation, Translation>>>,
  ) {
    // if any translation is plural, we are migrating key to plural
    // key which already exists in the real data (not just in the import) is plural
    val existingKey = importDataManager.existingKeys[data.key]
    val exitingKeyIsPlural = existingKey?.isPlural ?: false

    if (exitingKeyIsPlural) {
      migrateNewTranslationsToPlurals(data.value, existingKey?.pluralArgName)
      return
    }

    val anyNewTranslationIsPlural = data.value.any { (translation) -> translation.isPlural }
    if (!anyNewTranslationIsPlural) {
      return
    }

    val existingOrNewKey =
      data.value
        .first()
        .second.key
    existingOrNewKey.isPlural = true
    // now we have to migrate the new translations
    val pluralArgName = migrateNewTranslationsToPlurals(data.value, null)
    existingOrNewKey.pluralArgName = pluralArgName
    keysToSave.add(existingOrNewKey)

    // if the key was already existing, we need to migrate its existing translations
    if (existingKey != null) {
      existingKeysToMigrate[existingKey.id] = pluralArgName
      ignoreTranslationsForMigration.addAll(data.value.map { it.second.id })
    }
  }

  private fun migrateNewTranslationsToPlurals(
    translationPairs: List<Pair<ImportTranslation, Translation>>,
    pluralArgName: String?,
  ): String {
    val keyPluralArgName =
      pluralArgName ?: translationPairs
        .firstOrNull()
        ?.first
        ?.key
        ?.pluralArgName
    val map = translationPairs.associateWith { it.second.text }
    val conversionResult = map.convertToIcuPlurals(keyPluralArgName)
    conversionResult.convertedStrings.forEach {
      it.key.second.text = it.value
    }
    return conversionResult.argName
  }
}
