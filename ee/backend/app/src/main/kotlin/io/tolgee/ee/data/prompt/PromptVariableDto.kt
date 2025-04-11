package io.tolgee.ee.data.prompt

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

class PromptVariableDto(
  val name: String,
  val value: String? = null,
  val description: String? = null,
  @ArraySchema(
    arraySchema =
      Schema(
        description = "List of nested properties for this variable, allowing hierarchical structuring.",
        type = "array",
        nullable = true,
      ),
    schema =
      Schema(
        ref = "#/components/schemas/PromptVariableDto",
      ),
  ) val props: MutableList<PromptVariableDto>? = null,
)
