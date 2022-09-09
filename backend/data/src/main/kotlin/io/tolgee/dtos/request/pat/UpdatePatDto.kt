package io.tolgee.dtos.request.pat

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
data class UpdatePatDto(
  @Schema(description = "New description of the PAT")
  @field:NotBlank
  @field:Length(max = 250, min = 1)
  val description: String = "",
)
