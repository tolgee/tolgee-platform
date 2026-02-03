package io.tolgee.dtos.request.importKeysResolvable

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.service.dataImport.OverrideMode

data class SingleStepImportResolvableRequest(
  @field:Schema(
    description =
      "Some translations are forbidden or protected:\n\n" +
        "When set to `RECOMMENDED` it will fail for DISABLED translations " +
        "and protected REVIEWED translations.\n" +
        "When set to `ALL` it will fail for DISABLED translations, " +
        "but will try to update protected REVIEWED translations (fails only if user has no permission)\n",
  )
  val overrideMode: OverrideMode? = OverrideMode.RECOMMENDED,
  @Schema(
    description =
      "If `false`, import will apply all `non-failed` overrides and reports `unresolvedConflict`\n." +
        "If `true`, import will fail completely on unresolved conflict and won't apply any changes. " +
        "Unresolved conflicts are reported in the `params` of the error response",
  )
  val errorOnUnresolvedConflict: Boolean? = null,
  @get:Schema(
    description = "List of keys to import",
  )
  var keys: List<SingleStepImportResolvableItemRequest> = listOf(),
)
