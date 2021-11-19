package io.tolgee.dtos.response;

import io.tolgee.model.Permission;

public class PermissionDTO {
    private Long id;

    private Permission.ProjectPermissionType type;

    private String username;

    private Long userId;

    private String userFullName;

    public PermissionDTO(Long id, Permission.ProjectPermissionType type, String username, Long userId, String userFullName) {
        this.id = id;
        this.type = type;
        this.username = username;
        this.userId = userId;
        this.userFullName = userFullName;
    }

    public PermissionDTO() {
    }

    public static PermissionDTO fromEntity(Permission entity) {
        return new PermissionDTO(entity.getId(), entity.getType(), entity.getUser().getUsername(), entity.getUser().getId(), entity.getUser().getName());
    }

    public static PermissionDTOBuilder builder() {
        return new PermissionDTOBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public Permission.ProjectPermissionType getType() {
        return this.type;
    }

    public String getUsername() {
        return this.username;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUserFullName() {
        return this.userFullName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(Permission.ProjectPermissionType type) {
        this.type = type;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionDTO)) return false;
        final PermissionDTO other = (PermissionDTO) o;
        if (!other.canEqual(this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final Object this$userId = this.getUserId();
        final Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final Object this$userFullName = this.getUserFullName();
        final Object other$userFullName = other.getUserFullName();
        return this$userFullName == null ? other$userFullName == null : this$userFullName.equals(other$userFullName);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PermissionDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final Object $userFullName = this.getUserFullName();
        result = result * PRIME + ($userFullName == null ? 43 : $userFullName.hashCode());
        return result;
    }

    public String toString() {
        return "PermissionDTO(id=" + this.getId() + ", type=" + this.getType() + ", username=" + this.getUsername() + ", userId=" + this.getUserId() + ", userFullName=" + this.getUserFullName() + ")";
    }

    public static class PermissionDTOBuilder {
        private Long id;
        private Permission.ProjectPermissionType type;
        private String username;
        private Long userId;
        private String userFullName;

        PermissionDTOBuilder() {
        }

        public PermissionDTO.PermissionDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PermissionDTO.PermissionDTOBuilder type(Permission.ProjectPermissionType type) {
            this.type = type;
            return this;
        }

        public PermissionDTO.PermissionDTOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public PermissionDTO.PermissionDTOBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PermissionDTO.PermissionDTOBuilder userFullName(String userFullName) {
            this.userFullName = userFullName;
            return this;
        }

        public PermissionDTO build() {
            return new PermissionDTO(id, type, username, userId, userFullName);
        }

        public String toString() {
            return "PermissionDTO.PermissionDTOBuilder(id=" + this.id + ", type=" + this.type + ", username=" + this.username + ", userId=" + this.userId + ", userFullName=" + this.userFullName + ")";
        }
    }
}
