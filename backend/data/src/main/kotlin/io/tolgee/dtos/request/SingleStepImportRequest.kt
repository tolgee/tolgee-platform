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
        "When set to `OVERRIDE`, existing translations will be overwrote.\n\n" +
        "When set to `NO_FORCE`, error will be thrown on conflict.",
  )
  val forceMode: ForceMode = ForceMode.NO_FORCE

  @Schema(
    description =
      "Maps the languages from imported files to languages existing in the Tolgee platform.\n\n" +
        "Use this field only when your files contain multiple languages (e.g., XLIFF files).\n\n" +
        "Otherwise, use the `languageTag` property of `fileMappings`.\n\n" +
        "Example: In xliff files, there are `source-language` and `target-language` attributes defined on `file` " +
        "element. Using this field you can map source and target values to languages stored in the Tolgee Platform.",
  )
  val languageMappings: List<LanguageMapping>? = null

  override var overrideKeyDescriptions: Boolean = false
  override var convertPlaceholdersToIcu: Boolean = true

  @get:Schema(
    description = "If false, only updates keys, skipping the creation of new keys",
  )
  override var createNewKeys: Boolean = true

  @get:Schema(
    description = "Definition of mapping for each file to import.",
  )
  var fileMappings: List<ImportFileMapping> = listOf()

  @get:Schema(
    description =
      "Keys created by this import will be tagged with these tags. " +
        "It add tags only to new keys. The keys that already exist will not be tagged.",
  )
  var tagNewKeys: List<String> = listOf()

  @get:Schema(
    description = "If yes, keys from project that were not included in import will be deleted.",
  )
  var removeOtherKeys: Boolean? = false
}
