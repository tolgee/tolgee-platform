package io.tolgee.dtos.response

data class UserResponseDTO(
        var id: Long? = null,
        var name: String? = null,
        var username: String? = null,
        var emailAwaitingVerification: String? = null
)
