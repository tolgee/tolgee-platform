package io.polygloat.dtos.request

import io.polygloat.dtos.PathDTO
import javax.validation.constraints.NotBlank

data class EditSourceDTO(
        @field:NotBlank
        var oldFullPathString: String? = null,
        @field:NotBlank
        var newFullPathString: String? = null
) {

    val oldPathDto: PathDTO
        get() = PathDTO.fromFullPath(oldFullPathString)

    val newPathDto: PathDTO
        get() = PathDTO.fromFullPath(newFullPathString)

}