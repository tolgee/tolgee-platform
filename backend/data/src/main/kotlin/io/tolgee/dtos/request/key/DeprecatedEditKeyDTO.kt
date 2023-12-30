package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.PathDTO
import jakarta.validation.constraints.NotBlank

@Deprecated(message = "Ugly naming", ReplaceWith("io/tolgee/dtos/request/EditKeyDTO.kt"))
data class DeprecatedEditKeyDTO(
  @field:NotBlank
  var oldFullPathString: String = "",
  @field:NotBlank
  var newFullPathString: String = "",
) {
  @get:Hidden
  @get:JsonIgnore
  val oldPathDto: PathDTO
    get() = PathDTO.fromFullPath(oldFullPathString)

  @get:Hidden
  @get:JsonIgnore
  val newPathDto: PathDTO
    get() = PathDTO.fromFullPath(newFullPathString)
}
