package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

class SetProjectPromptCustomizationRequest(
  @Schema(
    description =
      "The project description used in the  prompt that " +
        "helps AI translator to understand the context of your project.",
    example =
      "We are Dunder Mifflin, a paper company. We sell paper. " +
        "This is an project of translations for out paper selling app.",
  )
  @Size(max = 2000)
  var description: String? = null,
)
