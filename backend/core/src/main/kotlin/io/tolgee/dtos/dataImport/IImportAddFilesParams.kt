package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter

interface IImportAddFilesParams {
  var structureDelimiter: Char?

  @get:Hidden
  @set:Hidden
  @get:Parameter(hidden = true)
  var storeFilesToFileStorage: Boolean

  companion object {
    const val STRUCTURE_DELIMITER_DESCRIPTION =
      "When importing files in structured formats (e.g., JSON, YAML), " +
        "this field defines the delimiter which will be used in names of imported keys."

    const val STRUCTURE_DELIMITER_EXAMPLE = "."
  }
}
