package io.tolgee.service.key.resolvableImport

import io.tolgee.model.key.Key
import io.tolgee.service.key.resolvableImport.ResolvableImporter.TranslationToModify

class ResolvableKeyImporterContext(val key: Key, val rootContext: ResolvableImportContext) {
  val wasPlural = key.isPlural
  val translationsToModify = mutableListOf<TranslationToModify>()
}
