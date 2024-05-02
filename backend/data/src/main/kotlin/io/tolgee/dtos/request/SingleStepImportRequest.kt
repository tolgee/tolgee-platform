package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IImportSettings
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.service.dataImport.ForceMode

class SingleStepImportRequest : ImportAddFilesParams(), IImportSettings {
  @Schema(
    description =
      "Whether to override existing translation data.\n\n" +
        "When set to `KEEP`, existing translations will be kept.\n\n" +
        "When set to `OVERRIDE`, existing translations will be overridden.\n\n" +
        "When set to `NO_FORCE`, error will be thrown on conflict.",
  )
  val forceMode: ForceMode = ForceMode.KEEP
  override var overrideKeyDescriptions: Boolean = false
  override var convertPlaceholdersToIcu: Boolean = true

  @get:Schema(
    description = "Definition of mapping for each file to import.",
  )
  var fileMappings: List<ImportFileMapping> = listOf()
}
