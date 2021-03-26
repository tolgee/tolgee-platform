package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.tolgee.dtos.PathDTO
import io.swagger.v3.oas.annotations.Hidden
import javax.validation.constraints.NotBlank

data class EditKeyDTO(
        @field:NotBlank
        var currentName: String? = null,
        @field:NotBlank
        var newName: String? = null
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
