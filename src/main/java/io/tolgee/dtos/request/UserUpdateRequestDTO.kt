package io.tolgee.dtos.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUpdateRequestDTO {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @Size(min = 8, max = 100)
    private String password;

    public UserUpdateRequestDTO(@NotBlank String name, @NotBlank @Email String email, @Size(min = 8, max = 100) String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public UserUpdateRequestDTO() {
    }

    public static UserUpdateRequestDTOBuilder builder() {
        return new UserUpdateRequestDTOBuilder();
    }

    public @NotBlank String getName() {
        return this.name;
    }

    public @NotBlank @Email String getEmail() {
        return this.email;
    }

    public @Size(min = 8, max = 100) String getPassword() {
        return this.password;
    }

    public void setName(@NotBlank String name) {
        this.name = name;
    }

    public void setEmail(@NotBlank @Email String email) {
        this.email = email;
    }

    public void setPassword(@Size(min = 8, max = 100) String password) {
        this.password = password;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UserUpdateRequestDTO)) return false;
        final UserUpdateRequestDTO other = (UserUpdateRequestDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$email = this.getEmail();
        final Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserUpdateRequestDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    public String toString() {
        return "UserUpdateRequestDTO(name=" + this.getName() + ", email=" + this.getEmail() + ", password=" + this.getPassword() + ")";
    }

    public static class UserUpdateRequestDTOBuilder {
        private @NotBlank String name;
        private @NotBlank @Email String email;
        private @Size(min = 8, max = 100) String password;

        UserUpdateRequestDTOBuilder() {
        }

        public UserUpdateRequestDTO.UserUpdateRequestDTOBuilder name(@NotBlank String name) {
            this.name = name;
            return this;
        }

        public UserUpdateRequestDTO.UserUpdateRequestDTOBuilder email(@NotBlank @Email String email) {
            this.email = email;
            return this;
        }

        public UserUpdateRequestDTO.UserUpdateRequestDTOBuilder password(@Size(min = 8, max = 100) String password) {
            this.password = password;
            return this;
        }

        public UserUpdateRequestDTO build() {
            return new UserUpdateRequestDTO(name, email, password);
        }

        public String toString() {
            return "UserUpdateRequestDTO.UserUpdateRequestDTOBuilder(name=" + this.name + ", email=" + this.email + ", password=" + this.password + ")";
        }
    }
}