package io.polygloat.dtos.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SignUpDto {
    @NotBlank
    String name;

    @Email
    @NotBlank
    String email;

    @Size(min = 8, max = 100)
    @NotBlank
    String password;

    String invitationCode;

    public SignUpDto(@NotBlank String name, @Email @NotBlank String email, @Size(min = 8, max = 100) @NotBlank String password, String invitationCode) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.invitationCode = invitationCode;
    }

    public SignUpDto() {
    }

    public static SignUpDtoBuilder builder() {
        return new SignUpDtoBuilder();
    }

    public @NotBlank String getName() {
        return this.name;
    }

    public @Email @NotBlank String getEmail() {
        return this.email;
    }

    public @Size(min = 8, max = 100) @NotBlank String getPassword() {
        return this.password;
    }

    public String getInvitationCode() {
        return this.invitationCode;
    }

    public void setName(@NotBlank String name) {
        this.name = name;
    }

    public void setEmail(@Email @NotBlank String email) {
        this.email = email;
    }

    public void setPassword(@Size(min = 8, max = 100) @NotBlank String password) {
        this.password = password;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SignUpDto)) return false;
        final SignUpDto other = (SignUpDto) o;
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
        final Object this$invitationCode = this.getInvitationCode();
        final Object other$invitationCode = other.getInvitationCode();
        if (this$invitationCode == null ? other$invitationCode != null : !this$invitationCode.equals(other$invitationCode)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SignUpDto;
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
        final Object $invitationCode = this.getInvitationCode();
        result = result * PRIME + ($invitationCode == null ? 43 : $invitationCode.hashCode());
        return result;
    }

    public String toString() {
        return "SignUpDto(name=" + this.getName() + ", email=" + this.getEmail() + ", password=" + this.getPassword() + ", invitationCode=" + this.getInvitationCode() + ")";
    }

    public static class SignUpDtoBuilder {
        private @NotBlank String name;
        private @Email @NotBlank String email;
        private @Size(min = 8, max = 100) @NotBlank String password;
        private String invitationCode;

        SignUpDtoBuilder() {
        }

        public SignUpDto.SignUpDtoBuilder name(@NotBlank String name) {
            this.name = name;
            return this;
        }

        public SignUpDto.SignUpDtoBuilder email(@Email @NotBlank String email) {
            this.email = email;
            return this;
        }

        public SignUpDto.SignUpDtoBuilder password(@Size(min = 8, max = 100) @NotBlank String password) {
            this.password = password;
            return this;
        }

        public SignUpDto.SignUpDtoBuilder invitationCode(String invitationCode) {
            this.invitationCode = invitationCode;
            return this;
        }

        public SignUpDto build() {
            return new SignUpDto(name, email, password, invitationCode);
        }

        public String toString() {
            return "SignUpDto.SignUpDtoBuilder(name=" + this.name + ", email=" + this.email + ", password=" + this.password + ", invitationCode=" + this.invitationCode + ")";
        }
    }
}