package io.tolgee.dtos.request.key

import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotBlank

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 2000)
  var name: String = "",

  @field:Length(max = 100)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  val namespace: String? = null,
)
