package io.tolgee.dtos.response;

import io.tolgee.model.UserAccount;

public class UserResponseDTO {

    private Long id;

    private String name;

    private String username;

    public UserResponseDTO(Long id, String name, String username) {
        this.id = id;
        this.name = name;
        this.username = username;
    }

    public UserResponseDTO() {
    }

    public static UserResponseDTO fromEntity(UserAccount user) {
        return UserResponseDTO.builder().username(user.getUsername()).name(user.getName()).id(user.getId()).build();
    }

    public static UserResponseDTOBuilder builder() {
        return new UserResponseDTOBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUsername() {
        return this.username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UserResponseDTO)) return false;
        final UserResponseDTO other = (UserResponseDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserResponseDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        return result;
    }

    public String toString() {
        return "UserResponseDTO(id=" + this.getId() + ", name=" + this.getName() + ", username=" + this.getUsername() + ")";
    }

    public static class UserResponseDTOBuilder {
        private Long id;
        private String name;
        private String username;

        UserResponseDTOBuilder() {
        }

        public UserResponseDTO.UserResponseDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserResponseDTO.UserResponseDTOBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserResponseDTO.UserResponseDTOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserResponseDTO build() {
            return new UserResponseDTO(id, name, username);
        }

        public String toString() {
            return "UserResponseDTO.UserResponseDTOBuilder(id=" + this.id + ", name=" + this.name + ", username=" + this.username + ")";
        }
    }
}
