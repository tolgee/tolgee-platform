package io.tolgee.dtos.dataImport

import io.swagger.v3.oas.annotations.Hidden

interface IImportAddFilesParams {
  var structureDelimiter: Char?

  @set:Hidden
  var storeFilesToFileStorage: Boolean
}
