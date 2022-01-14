package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.PathDTO
import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotBlank

data class OldEditKeyDto(
  @field:NotBlank
  var currentName: String = "",
  @field:NotBlank
  @field:Length(max = 200)
  var newName: String = ""
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
