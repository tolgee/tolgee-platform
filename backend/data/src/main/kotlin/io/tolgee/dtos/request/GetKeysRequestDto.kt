package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotEmpty

class GetKeysRequestDto {
  var keys: List<KeyDefinitionDto> = listOf()
  @get:NotEmpty
  @Schema(description = "Tags to return language translations in")
  var languageTags: List<String> = listOf()
}
