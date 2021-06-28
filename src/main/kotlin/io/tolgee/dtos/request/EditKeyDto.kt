package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.dtos.PathDTO
import javax.validation.constraints.NotBlank

data class EditKeyDto(
        @field:NotBlank
        var currentName: String = "",
        @field:NotBlank
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
