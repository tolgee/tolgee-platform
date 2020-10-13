package com.polygloat.dtos.request;

import com.polygloat.model.Permission;
import lombok.Data;

@Data
public class InviteUser {
    private Long repositoryId;
    private Permission.RepositoryPermissionType type;
}
