package io.tolgee.dtos.request

import javax.validation.constraints.NotBlank

data class GenerateAddressPathDto(
        val oldAddressPart: String? = null,

        @field:NotBlank
        var name: String? = null
)
