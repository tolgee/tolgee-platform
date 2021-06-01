package io.tolgee.dtos.request

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class EditRepositoryDTO(
        @field:NotNull
        var projectId: Long? = null,

        @field:NotNull @field:Size(min = 3, max = 500)
        var name: String? = null,

        @field:Size(min = 3, max = 60)
        @field:Pattern(regexp = "^[a-z0-9]*[a-z]+[a-z0-9]*$", message = "invalid_pattern")
        var addressPart: String? = null
)
