package io.tolgee.dtos.request.translation.importKeysResolvable

import io.swagger.v3.oas.annotations.media.Schema

class ImportTranslationResolvableDto(
  /**
   * Key full path is stored as name in entity
   */
  @Schema(description = "Translation text", example = "Hello! I am a translation!")
  val text: String = "",
  @Schema(
    description = """Determines, how conflict is resolved.

- KEEP: Translation is not changed
- OVERRIDE: Translation is overridden
- NEW: New translation is created
- FORCE_OVERRIDE: Translation is updated, created or kept.
""",
    example = "OVERRIDE",
  )
  val resolution: ImportTranslationResolution = ImportTranslationResolution.NEW,
)
