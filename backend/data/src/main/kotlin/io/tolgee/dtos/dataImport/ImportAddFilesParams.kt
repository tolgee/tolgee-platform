package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Parameter

class ImportAddFilesParams(
  @field:Parameter(
    description =
      "When importing structured JSONs, you can set " +
        "the delimiter which will be used in names of improted keys.",
  )
  var structureDelimiter: Char? = '.',

  var storeFilesToFileStorage: Boolean = true
)
