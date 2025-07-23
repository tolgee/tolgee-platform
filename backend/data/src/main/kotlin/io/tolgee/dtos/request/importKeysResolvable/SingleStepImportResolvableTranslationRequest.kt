package io.tolgee.dtos.request.importKeysResolvable

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.service.dataImport.ForceMode

class SingleStepImportResolvableTranslationRequest(
  /**
   * Key full path is stored as name in entity
   */
  @Schema(description = "Translation text", example = "Hello! I am a translation!")
  val text: String = "",
  @Schema(
    description = """Determines, how conflict is resolved.
      - KEEP: Translation is not changed
      - OVERRIDE: Translation is overridden
      - NO_FORCE: Fail in case of conflict
    """,
    example = "OVERRIDE",
  )
  val forceMode: ForceMode = ForceMode.NO_FORCE,
)
