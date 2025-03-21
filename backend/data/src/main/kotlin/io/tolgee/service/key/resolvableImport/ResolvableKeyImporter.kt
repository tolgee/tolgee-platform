package io.tolgee.service.key.resolvableImport

import io.tolgee.constants.Message
import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.key.Key

class ResolvableKeyImporter(
  private val keyToImport: ImportKeysResolvableItemDto,
  private val rootContext: ResolvableImportContext,
) {
  fun import(): Key {
    keyToImport.mapLanguageAsKey().forEach translations@{ (language, dto) ->
      language ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
      ResolvableTranslationImporter(keyContext, rootContext, language, dto).import()
    }

    handlePluralization()
    rootContext.saveTranslations(keyContext.translationsToModify)
    return key
  }

  private fun handlePluralization() {
    ResolvableImportPluralizationHandler(keyContext).handle()
  }

  private fun ImportKeysResolvableItemDto.mapLanguageAsKey() =
    translations.mapNotNull { (languageTag, value) ->
      value ?: return@mapNotNull null
      rootContext.languages[languageTag] to value
    }

  private val keyContext by lazy {
    ResolvableKeyImporterContext(keyToImport, key, rootContext)
  }

  private val key by lazy {
    rootContext.getOrCreateKey(keyToImport).first
  }
}
