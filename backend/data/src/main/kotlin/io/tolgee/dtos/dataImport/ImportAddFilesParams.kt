package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.dtos.request.ImportFileMapping

class ImportAddFilesParams(
  @field:Parameter(
    description =
      "When importing structured JSONs, you can set " +
        "the delimiter which will be used in names of improted keys.",
  )
  override var structureDelimiter: Char? = '.',
  @field:Hidden
  override var storeFilesToFileStorage: Boolean = true,
  @field:Parameter(
    description =
      "If true, for structured formats (like JSON) arrays are supported. " +
        "e.g. Array object like {\"hello\": [\"item1\", \"item2\"]} will be imported as keys " +
        "hello[0] = \"item1\" and hello[1] = \"item2\".",
  )
  var supportArrays: Boolean = true,
  override var fileMappings: List<ImportFileMapping> = listOf(),
) : IImportAddFilesParams
