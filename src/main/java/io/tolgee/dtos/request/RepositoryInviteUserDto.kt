package io.tolgee.dtos.request

import io.tolgee.model.Permission.ProjectPermissionType
import javax.validation.constraints.NotNull

data class RepositoryInviteUserDto(
        @field:NotNull
        var projectId: Long? = null,
        @field:NotNull
        var type: ProjectPermissionType? = null
)
