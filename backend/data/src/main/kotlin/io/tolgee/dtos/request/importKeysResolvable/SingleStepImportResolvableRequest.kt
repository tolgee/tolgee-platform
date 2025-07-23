package io.tolgee.dtos.request.importKeysResolvable

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.IImportSettings
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.service.dataImport.OverrideMode

data class SingleStepImportResolvableRequest(
  @Schema(
    description =
      "Some translations are forbidden or protected:\n\n" +
        "When set to `RECOMMENDED` it will fail for DISABLED translations " +
        "and protected REVIEWED translations.\n" +
        "When set to `ALL` it will fail for DISABLED translations, " +
        "but will try to update protected REVIEWED translations (fails only if user has no permission)\n"
  )
  val overrideMode: OverrideMode = OverrideMode.RECOMMENDED,

  @Schema(
    description =
      "If `false`, import will apply all `non-failed` overrides and reports `failedKeys`\n." +
        "If `true`, import will fail completely on failed override and won't apply any changes. " +
        "Failed keys are reported in the `params` of the error response"
  )
  val errorOnFailedKey: Boolean? = null,

  override var overrideKeyDescriptions: Boolean = false,
  override var convertPlaceholdersToIcu: Boolean = true,

  @get:Schema(
    description = "If false, only updates keys, skipping the creation of new keys",
  )
  override var createNewKeys: Boolean = true,

  @get:Schema(
    description =
      "Keys created by this import will be tagged with these tags. " +
        "It add tags only to new keys. The keys that already exist will not be tagged.",
  )
  var tagNewKeys: List<String> = listOf(),

  @get:Schema(
    description = "If yes, keys from project that were not included in import will be deleted " +
      "(only within namespaces which are included in the import).",
  )
  var removeOtherKeys: Boolean? = false,

  @get:Schema(
    description = "List of keys to import",
  )
  var keys: List<SingleStepImportResolvableItemRequest> = listOf()
) : ImportAddFilesParams(), IImportSettings
