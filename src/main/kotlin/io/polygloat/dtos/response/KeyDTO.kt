package io.polygloat.dtos.response

import io.polygloat.dtos.PathDTO
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class KeyDTO(
        @field:NotBlank
        @field:Size(min = 1, max = 300)
        var fullPathString: String? = null
) {
    val pathDto: PathDTO
        get() = PathDTO.fromFullPath(fullPathString)
}