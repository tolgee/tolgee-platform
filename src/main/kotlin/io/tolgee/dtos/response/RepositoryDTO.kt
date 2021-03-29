package io.tolgee.dtos.response

import io.tolgee.model.Permission.RepositoryPermissionType
import io.tolgee.model.Repository

data class RepositoryDTO(var id: Long? = null,
                         var name: String? = null,
                         var permissionType: RepositoryPermissionType? = null) {

    companion object {
        @JvmStatic
        fun fromEntityAndPermission(repository: Repository, permissionType: RepositoryPermissionType): RepositoryDTO {
            return RepositoryDTO(repository.id, repository.name, permissionType)
        }
    }
}
