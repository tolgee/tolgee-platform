package io.tolgee.service.key.resolvableImport

import io.tolgee.dtos.request.translation.importKeysResolvable.ImportKeysResolvableItemDto
import io.tolgee.model.key.Key
import io.tolgee.service.key.resolvableImport.ResolvableImporter.TranslationToModify

class ResolvableKeyImporterContext(
  val keyToImport: ImportKeysResolvableItemDto,
  val key: Key,
  val rootContext: ResolvableImportContext
) {
  val wasPlural = key.isPlural
  val translationsToModify = mutableListOf<TranslationToModify>()
}
