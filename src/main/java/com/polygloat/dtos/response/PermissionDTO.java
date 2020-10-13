package com.polygloat.dtos.response;

import com.polygloat.model.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionDTO {
    private Long id;

    private Permission.RepositoryPermissionType type;

    private String username;

    private Long userId;

    private String userFullName;

    public static PermissionDTO fromEntity(Permission entity) {
        return new PermissionDTO(entity.getId(), entity.getType(), entity.getUser().getUsername(), entity.getUser().getId(), entity.getUser().getName());
    }
}
