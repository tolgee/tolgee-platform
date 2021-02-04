package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.tolgee.dtos.PathDTO
import io.swagger.v3.oas.annotations.Hidden
import javax.validation.constraints.NotBlank

data class EditKeyDTO(
        @field:NotBlank
        var oldFullPathString: String? = null,
        @field:NotBlank
        var newFullPathString: String? = null
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