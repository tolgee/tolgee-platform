package io.tolgee.dtos.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.PathDTO
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Deprecated(message = "Ugly parameter name")
data class DeprecatedKeyDto(
  @field:NotBlank
  @field:Size(min = 1, max = 300)
  @Schema(description = "This means name of key. Will be renamed in v2")
  var fullPathString: String = "",
) {
  @get:JsonIgnore
  val pathDto: PathDTO
    get() = PathDTO.fromFullPath(fullPathString)
}
