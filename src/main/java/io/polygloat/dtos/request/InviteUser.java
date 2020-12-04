package io.polygloat.dtos.request;

import io.polygloat.model.Permission;

public class InviteUser {
    private Long repositoryId;
    private Permission.RepositoryPermissionType type;

    public InviteUser() {
    }

    public Long getRepositoryId() {
        return this.repositoryId;
    }

    public Permission.RepositoryPermissionType getType() {
        return this.type;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setType(Permission.RepositoryPermissionType type) {
        this.type = type;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof InviteUser)) return false;
        final InviteUser other = (InviteUser) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$repositoryId = this.getRepositoryId();
        final Object other$repositoryId = other.getRepositoryId();
        if (this$repositoryId == null ? other$repositoryId != null : !this$repositoryId.equals(other$repositoryId)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof InviteUser;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $repositoryId = this.getRepositoryId();
        result = result * PRIME + ($repositoryId == null ? 43 : $repositoryId.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        return result;
    }

    public String toString() {
        return "InviteUser(repositoryId=" + this.getRepositoryId() + ", type=" + this.getType() + ")";
    }
}
