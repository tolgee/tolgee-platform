package io.tolgee.service.dataImport

import io.tolgee.formats.convertToIcuPlural
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.translation.Translation
import io.tolgee.service.translation.TranslationService

class PluralizationHandler(
  private val importDataManager: ImportDataManager,
  private val storedDataImporter: StoredDataImporter,
  private val translationService: TranslationService,
) {
  private val existingKeysToMigrate: MutableList<Long> = mutableListOf()
  private val ignoreTranslationsForMigration: MutableList<Long> = mutableListOf()

  fun handlePluralization() {
    val byKey =
      storedDataImporter.translationsToSave
        .groupBy { it.second.key.namespace?.name to it.second.key.name }
    byKey.forEach {
      handleKeyPluralization(it)
    }

    translationService.onKeyIsPluralChanged(existingKeysToMigrate, true, ignoreTranslationsForMigration)
  }

  private fun handleKeyPluralization(
    data: Map.Entry<Pair<String?, String>, List<Pair<ImportTranslation, Translation>>>,
  ) {
    // if any translation is plural, we are migrating key to plural
    // key which already exists in the real data (not just in the import) is plural
    val exitingKeyIsPlural = importDataManager.existingKeys[data.key]?.isPlural ?: false

    if (exitingKeyIsPlural) {
      migrateNewTranslationsToPlurals(data.value)
      return
    }

    val anyNewTranslationIsPlural = data.value.any { (translation) -> translation.isPlural }
    if (!anyNewTranslationIsPlural) {
      return
    }

    data.value.first().second.key.isPlural = true
    // now we have to migrate the new translations
    migrateNewTranslationsToPlurals(data.value)

    // if the key was already existing, we need to migrate its existing translations
    val existingKey = importDataManager.existingKeys[data.key]
    if (existingKey != null) {
      existingKeysToMigrate.add(existingKey.id)
      ignoreTranslationsForMigration.addAll(data.value.map { it.second.id })
    }
  }

  private fun migrateNewTranslationsToPlurals(translationPairs: List<Pair<ImportTranslation, Translation>>) {
    translationPairs.forEach {
      if (it.first.isPlural) {
        return@forEach
      }
      it.second.text = it.second.text?.convertToIcuPlural()
    }
  }
}
