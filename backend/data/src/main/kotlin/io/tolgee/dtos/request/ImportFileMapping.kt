package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.formats.importCommon.ImportFormat

class ImportFileMapping(
  @Schema(
    description =
      "Name of the file to import. " +
        "This is the name of the file provided in `files` request part or in uploaded archive.",
  )
  val fileName: String,
  @Schema(
    description =
      "This field maps the languages from imported files to languages existing in the Tolgee platform. " +
        "If this field is null, Tolgee will try to guess the language from the file name or file contents.\n\n" +
        "It is recommended to provide these values, to prevent any issues with language detection.",
  )
  val languageMappings: List<LanguageMapping>? = null,
  @Schema(
    description = "Namespace to import the file to. If not provided, the key will be imported without namespace.",
  )
  val namespace: String? = null,
  @Schema(
    description =
      "Format of the file. If not provided, " +
        "Tolgee will try to guess the format from the file name or file contents.\n\n" +
        "It is recommended to provide these values, to prevent any issues with format detection.",
  )
  val format: ImportFormat? = null,
)
