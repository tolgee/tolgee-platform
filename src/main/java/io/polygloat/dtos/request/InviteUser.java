package io.polygloat.dtos.request;

import io.polygloat.model.Permission;
import lombok.Data;

@Data
public class InviteUser {
    private Long repositoryId;
    private Permission.RepositoryPermissionType type;
}
