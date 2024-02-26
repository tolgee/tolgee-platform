package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter

class ImportAddFilesParams(
  @field:Parameter(
    description =
      "When importing structured JSONs, you can set " +
        "the delimiter which will be used in names of improted keys.",
  )
  override var structureDelimiter: Char? = '.',
  @field:Hidden
  override var storeFilesToFileStorage: Boolean = true,
) : IImportAddFilesParams
