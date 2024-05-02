package io.tolgee.dtos.request

import io.tolgee.formats.importCommon.ImportFormat

class SingleStepImportPathMapping(
  val fileName: String,
  val languageMappings: List<SingleStepImportLanguageMapping>? = null,
  val namespace: String? = null,
  val format: ImportFormat? = null,
)
