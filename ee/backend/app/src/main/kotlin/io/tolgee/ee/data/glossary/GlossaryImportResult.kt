package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema

class GlossaryImportResult(
  @Schema(description = "Number of imported terms", example = "42")
  val imported: Int,
)
