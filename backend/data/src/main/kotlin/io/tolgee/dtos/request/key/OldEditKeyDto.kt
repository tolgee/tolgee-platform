package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.PathDTO
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class OldEditKeyDto(
  @field:NotBlank
  var currentName: String = "",
  @field:NotBlank
  @field:Length(max = 2000)
  var newName: String = "",
) {
  @get:Hidden
  @get:JsonIgnore
  val oldPathDto: PathDTO
    get() = PathDTO.fromFullPath(currentName)

  @get:Hidden
  @get:JsonIgnore
  val newPathDto: PathDTO
    get() = PathDTO.fromFullPath(newName)
}
