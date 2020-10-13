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
public class InvitationDTO {

    private Long id;

    private String code;

    private Permission.RepositoryPermissionType type;

    public static InvitationDTO fromEntity(com.polygloat.model.Invitation invitation) {
        return InvitationDTO.builder().id(invitation.getId()).code(invitation.getCode()).type(invitation.getPermission().getType()).build();
    }
}
