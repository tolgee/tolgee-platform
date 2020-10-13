package com.polygloat.dtos.response;

import com.polygloat.exceptions.InvalidStateException;
import com.polygloat.model.Permission;
import com.polygloat.model.Repository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryDTO {
    Long id;

    String name;

    Permission.RepositoryPermissionType permissionType;

    public static RepositoryDTO fromEntityAndPermission(Repository repository, Permission permission) {
        return new RepositoryDTO(repository.getId(), repository.getName(), permission.getType());
    }
}
