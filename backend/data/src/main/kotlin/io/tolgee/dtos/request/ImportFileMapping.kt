package io.tolgee.dtos.request

import io.tolgee.formats.importCommon.ImportFormat

class ImportFileMapping(
  val fileName: String,
  val languageMappings: List<LanguageMapping>? = null,
  val namespace: String? = null,
  val format: ImportFormat? = null,
)
