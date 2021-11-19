package io.tolgee.dtos.response;

import io.tolgee.model.Invitation;
import io.tolgee.model.Permission;

public class InvitationDTO {

    private Long id;

    private String code;

    private Permission.ProjectPermissionType type;

    public InvitationDTO(Long id, String code, Permission.ProjectPermissionType type) {
        this.id = id;
        this.code = code;
        this.type = type;
    }

    public InvitationDTO() {
    }

    public static InvitationDTO fromEntity(Invitation invitation) {
        return InvitationDTO.builder().id(invitation.getId()).code(invitation.getCode()).type(invitation.getPermission().getType()).build();
    }

    public static InvitationDTOBuilder builder() {
        return new InvitationDTOBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getCode() {
        return this.code;
    }

    public Permission.ProjectPermissionType getType() {
        return this.type;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setType(Permission.ProjectPermissionType type) {
        this.type = type;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof InvitationDTO)) return false;
        final InvitationDTO other = (InvitationDTO) o;
        if (!other.canEqual(this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$code = this.getCode();
        final Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        return this$type == null ? other$type == null : this$type.equals(other$type);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof InvitationDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        return result;
    }

    public String toString() {
        return "InvitationDTO(id=" + this.getId() + ", code=" + this.getCode() + ", type=" + this.getType() + ")";
    }

    public static class InvitationDTOBuilder {
        private Long id;
        private String code;
        private Permission.ProjectPermissionType type;

        InvitationDTOBuilder() {
        }

        public InvitationDTO.InvitationDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InvitationDTO.InvitationDTOBuilder code(String code) {
            this.code = code;
            return this;
        }

        public InvitationDTO.InvitationDTOBuilder type(Permission.ProjectPermissionType type) {
            this.type = type;
            return this;
        }

        public InvitationDTO build() {
            return new InvitationDTO(id, code, type);
        }

        public String toString() {
            return "InvitationDTO.InvitationDTOBuilder(id=" + this.id + ", code=" + this.code + ", type=" + this.type + ")";
        }
    }
}
