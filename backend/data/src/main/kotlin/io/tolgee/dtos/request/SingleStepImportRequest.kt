package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.api.IImportSettings
import io.tolgee.dtos.dataImport.IImportAddFilesParams
import io.tolgee.service.dataImport.ForceMode

class SingleStepImportRequest : IImportAddFilesParams, IImportSettings {
  val forceMode: ForceMode = ForceMode.KEEP

  @field:Parameter(
    description =
      "When importing structured JSONs, you can set " +
        "the delimiter which will be used in names of improted keys.",
  )
  override var structureDelimiter: Char? = '.'

  @field:Hidden
  override var storeFilesToFileStorage: Boolean = true
  override var overrideKeyDescriptions: Boolean = false
  override var convertPlaceholdersToIcu: Boolean = true
}
