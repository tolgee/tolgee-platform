package io.tolgee.service.key.resolvableImport

import io.tolgee.constants.Message
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.formats.convertToIcuPlurals
import io.tolgee.formats.convertToPluralIfAnyIsPlural
import io.tolgee.model.key.Key
import io.tolgee.service.key.resolvableImport.ResolvableImporter.TranslationToModify

class ResolvableKeyImporter(
  private val keyToImport: ImportKeysResolvableItemDto,
  private val rootContext: ResolvableImportContext,
) {
  private val key by lazy {
    rootContext.getOrCreateKey(keyToImport).first
  }

  fun import(): Key {
    val keyContext = ResolvableKeyImporterContext(key, rootContext)

    keyToImport.mapLanguageAsKey().forEach translations@{ (language, dto) ->
      language ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
      ResolvableTranslationImporter(keyContext, rootContext, language, dto).import()
    }

    handlePluralizationAndSave(keyContext.wasPlural, keyContext.translationsToModify)
    return key
  }

  private fun handlePluralizationAndSave(
    wasPlural: Boolean,
    translationsToModify: MutableList<TranslationToModify>,
  ) {
    val translationsToModifyMap = translationsToModify.associateWith { it.text }

    // when existing key is plural, we are converting all to plurals
    if (wasPlural) {
      translationsToModifyMap.convertToIcuPlurals(null).convertedStrings.forEach {
        it.key.text = it.value
      }
      rootContext.saveTranslations(translationsToModify)
      return
    }

    val convertedToPlurals =
      translationsToModifyMap.convertToPluralIfAnyIsPlural()

    // if anything from the new translations is plural, we are converting the key to plural
    if (convertedToPlurals != null) {
      key.isPlural = true
      rootContext.keyService.save(key)
      translationsToModify.forEach { translation ->
        translation.text = convertedToPlurals.convertedStrings[translation]
      }
      // now we have to also handle translations of keys,
      // which are already existing in the database
      rootContext.isPluralChangedForKeys[key.id] = convertedToPlurals.argName
    }

    rootContext.saveTranslations(translationsToModify)
  }

  private fun ImportKeysResolvableItemDto.mapLanguageAsKey() =
    translations.mapNotNull { (languageTag, value) ->
      value ?: return@mapNotNull null
      rootContext.languages[languageTag] to value
    }
}
