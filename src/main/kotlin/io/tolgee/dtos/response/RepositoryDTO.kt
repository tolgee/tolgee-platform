package io.tolgee.dtos.response

import io.tolgee.model.Permission
import io.tolgee.model.Permission.RepositoryPermissionType
import io.tolgee.model.Repository

data class RepositoryDTO(var id: Long? = null,
                         var name: String? = null,
                         var permissionType: RepositoryPermissionType? = null) {

    companion object {
        @JvmStatic
        fun fromEntityAndPermission(repository: Repository, permission: Permission): RepositoryDTO {
            return RepositoryDTO(repository.id, repository.name, permission.type)
        }
    }
}