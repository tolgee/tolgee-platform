package io.polygloat.dtos.response

import io.polygloat.model.Permission
import io.polygloat.model.Permission.RepositoryPermissionType
import io.polygloat.model.Repository

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