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
    description = "Namespace to import the file to. If not provided, the key will be imported without namespace.",
  )
  val namespace: String? = null,
  @Schema(
    description =
      "Format of the file. If not provided, " +
        "Tolgee will try to guess the format from the file name or file contents.\n\n" +
        "It is recommended to provide these values to prevent any issues with format detection.",
  )
  val format: ImportFormat? = null,
  @Schema(
    description =
      "The existing language tag in the Tolgee platform to which the imported language should be mapped.\n\n" +
        "When null, Tolgee will try to guess the language from the file contents or file name.",
  )
  val languageTag: String? = null,
  @Schema(
    description =
      "Tags of languages to be imported. When null, all languages from will be imported.\n\n" +
        "This field is useful when the file contains multiple languages and you want to import only some of them. " +
        "For example when importing Apple String Catalog (APPLE_XCSTRINGS), you might want only to import the base " +
        "language.",
  )
  val languageTagsToImport: List<String>? = null,
)
