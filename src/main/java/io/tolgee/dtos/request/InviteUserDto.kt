package io.tolgee.dtos.request

import io.tolgee.model.Permission.RepositoryPermissionType
import javax.validation.constraints.NotNull

data class InviteUserDto(
        @field:NotNull
        var repositoryId: Long? = null,
        @field:NotNull
        var type: RepositoryPermissionType? = null
)
