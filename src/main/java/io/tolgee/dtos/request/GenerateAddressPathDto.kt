package io.tolgee.dtos.request

import javax.validation.constraints.NotBlank

data class GenerateAddressPathDto(
        @field:NotBlank
        var name: String? = null,

        val oldAddressPart: String? = null,
)
