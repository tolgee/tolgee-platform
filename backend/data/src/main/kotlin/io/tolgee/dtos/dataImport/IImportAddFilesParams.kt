package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.request.ImportFileMapping

interface IImportAddFilesParams {
  var structureDelimiter: Char?

  @set:Hidden
  var storeFilesToFileStorage: Boolean

  @get:Schema(
    description = "Map of filename mapping to import mappings",
  )
  var fileMappings: List<ImportFileMapping>
}
