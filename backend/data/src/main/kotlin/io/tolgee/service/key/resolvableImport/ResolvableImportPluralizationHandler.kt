package io.tolgee.service.key.resolvableImport

import io.tolgee.formats.convertToIcuPlurals
import io.tolgee.formats.convertToPluralIfAnyIsPlural
import io.tolgee.service.translation.TranslationService

class ResolvableImportPluralizationHandler(
  private val keyContext: ResolvableKeyImporterContext
) {
  fun handle() {
    if (keyContext.keyToImport.isPlural == null) {
      handleByAutoDetection()
      return
    }
    handleByUserInput()
  }

  private fun handleByUserInput() {
    if (keyContext.wasPlural == keyContext.keyToImport.isPlural) {
      return
    }

    val isPlural = keyContext.keyToImport.isPlural!!
    // TODO: Handle the plural arg guessing and test it also for key creation / updates
    val normalized = translationService.validateAndNormalizePlurals(map, isPlural, keyContext.keyToImport.pluralArgName)
    keyContext.translationsToModify.forEach { translation ->
      translation.text = normalized.convertedStrings[translation]
    }
    markKeyForPluralChange(normalized.argName)
  }

  private fun handleByAutoDetection() {
    // when existing key is plural, we are converting all to plurals
    if (keyContext.wasPlural) {
      map.convertToIcuPlurals(null)
        .convertedStrings
        .forEach { (translation, newText) ->
          translation.text = newText
        }
      return
    }

    val convertedToPlurals =
      map.convertToPluralIfAnyIsPlural()

    // if anything from the new translations is plural, we are converting the key to plural
    if (convertedToPlurals != null) {
      keyContext.key.isPlural = true
      keyContext.rootContext.keyService.save(keyContext.key)

      keyContext.translationsToModify.forEach { translation ->
        translation.text = convertedToPlurals.convertedStrings[translation]
      }

      markKeyForPluralChange(convertedToPlurals.argName)
    }
  }

  private fun markKeyForPluralChange(argName: String?) {
    // now we have to also handle translations of keys,
    // which are already existing in the database
    keyContext.rootContext.isPluralChangedForKeys[keyContext.key.id] = argName

  }

  private val map by lazy {
    keyContext.translationsToModify.associateWith { it.text }
  }

  private val translationService: TranslationService get() = keyContext.rootContext.translationService
}
