package io.tolgee.dtos.request

import io.tolgee.model.Permission
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class EditOrganizationDto(
        @field:NotNull
        var id: Long? = null,

        @field:NotBlank @field:Size(min = 3, max = 50)
        var name: String? = null,

        var description: String? = null,

        @field:NotBlank @field:Size(min = 3, max = 60)
        var addressPart: String? = null,

        @Enumerated(EnumType.STRING)
        var basePermissions: Permission.RepositoryPermissionType = Permission.RepositoryPermissionType.VIEW,
)
