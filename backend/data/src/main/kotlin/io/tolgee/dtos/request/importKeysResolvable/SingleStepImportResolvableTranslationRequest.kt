package io.tolgee.dtos.request.importKeysResolvable

import io.swagger.v3.oas.annotations.media.Schema

class SingleStepImportResolvableTranslationRequest(
  @Schema(description = "Translation text", example = "Hello! I am a translation!")
  val text: String = "",
  @Schema(
    description = """
    To ensure the import doesn't override something that should not be (in case data have changed unexpectedly),
    you can specify what do you "expect":
      - EXPECT_NO_CONFLICT: There should be no conflict, if there is, import fails
      - OVERRIDE: New translation is applied over the existing in every case (Default)
    """,
    example = "OVERRIDE",
  )
  val resolution: ResolvableTranslationResolution? = ResolvableTranslationResolution.OVERRIDE,
)
