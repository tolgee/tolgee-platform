package io.polygloat.dtos.request;

import io.polygloat.model.Permission;

public class PermissionEditDto {
    private Long permissionId;
    private Permission.RepositoryPermissionType type;

    public PermissionEditDto(Long permissionId, Permission.RepositoryPermissionType type) {
        this.permissionId = permissionId;
        this.type = type;
    }

    public PermissionEditDto() {
    }

    public static PermissionEditDtoBuilder builder() {
        return new PermissionEditDtoBuilder();
    }

    public Long getPermissionId() {
        return this.permissionId;
    }

    public Permission.RepositoryPermissionType getType() {
        return this.type;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public void setType(Permission.RepositoryPermissionType type) {
        this.type = type;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionEditDto)) return false;
        final PermissionEditDto other = (PermissionEditDto) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$permissionId = this.getPermissionId();
        final Object other$permissionId = other.getPermissionId();
        if (this$permissionId == null ? other$permissionId != null : !this$permissionId.equals(other$permissionId)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PermissionEditDto;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $permissionId = this.getPermissionId();
        result = result * PRIME + ($permissionId == null ? 43 : $permissionId.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        return result;
    }

    public String toString() {
        return "PermissionEditDto(permissionId=" + this.getPermissionId() + ", type=" + this.getType() + ")";
    }

    public static class PermissionEditDtoBuilder {
        private Long permissionId;
        private Permission.RepositoryPermissionType type;

        PermissionEditDtoBuilder() {
        }

        public PermissionEditDto.PermissionEditDtoBuilder permissionId(Long permissionId) {
            this.permissionId = permissionId;
            return this;
        }

        public PermissionEditDto.PermissionEditDtoBuilder type(Permission.RepositoryPermissionType type) {
            this.type = type;
            return this;
        }

        public PermissionEditDto build() {
            return new PermissionEditDto(permissionId, type);
        }

        public String toString() {
            return "PermissionEditDto.PermissionEditDtoBuilder(permissionId=" + this.permissionId + ", type=" + this.type + ")";
        }
    }
}
