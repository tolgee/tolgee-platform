package io.polygloat.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username"}, name = "useraccount_username"),
        @UniqueConstraint(columnNames = {"third_party_auth_type", "third_party_auth_id"}, name = "useraccount_authtype_auth_id"),

})
public class UserAccount extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String name;

    @OneToMany(mappedBy = "createdBy")
    private Set<Repository> createdRepositories = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Permission> permissions;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(name = "third_party_auth_type")
    private String thirdPartyAuthType;

    @Column(name = "third_party_auth_id")
    private String thirdPartyAuthId;

    @Column(name = "reset_password_code")
    private String resetPasswordCode;


    public UserAccount() {
    }

    public UserAccount(String username) {
        this.username = username;
    }

    public UserAccount(Long id, String username, String password, String name, Set<Repository> createdRepositories, Set<Permission> permissions, Role role, String thirdPartyAuthType, String thirdPartyAuthId, String resetPasswordCode) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.createdRepositories = createdRepositories;
        this.permissions = permissions;
        this.role = role;
        this.thirdPartyAuthType = thirdPartyAuthType;
        this.thirdPartyAuthId = thirdPartyAuthId;
        this.resetPasswordCode = resetPasswordCode;
    }

    private static Set<Repository> $default$createdRepositories() {
        return new HashSet<>();
    }

    private static Role $default$role() {
        return Role.USER;
    }

    public static UserAccountBuilder builder() {
        return new UserAccountBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getName() {
        return this.name;
    }

    public Set<Repository> getCreatedRepositories() {
        return this.createdRepositories;
    }

    public Set<Permission> getPermissions() {
        return this.permissions;
    }

    public Role getRole() {
        return this.role;
    }

    public String getThirdPartyAuthType() {
        return this.thirdPartyAuthType;
    }

    public String getThirdPartyAuthId() {
        return this.thirdPartyAuthId;
    }

    public String getResetPasswordCode() {
        return this.resetPasswordCode;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatedRepositories(Set<Repository> createdRepositories) {
        this.createdRepositories = createdRepositories;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setThirdPartyAuthType(String thirdPartyAuthType) {
        this.thirdPartyAuthType = thirdPartyAuthType;
    }

    public void setThirdPartyAuthId(String thirdPartyAuthId) {
        this.thirdPartyAuthId = thirdPartyAuthId;
    }

    public void setResetPasswordCode(String resetPasswordCode) {
        this.resetPasswordCode = resetPasswordCode;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UserAccount)) return false;
        final UserAccount other = (UserAccount) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UserAccount;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    public String toString() {
        return "UserAccount(id=" + this.getId() + ", username=" + this.getUsername() + ", name=" + this.getName() + ")";
    }

    public enum Role {
        USER,
        ADMIN
    }

    public static class UserAccountBuilder {
        private Long id;
        private String username;
        private String password;
        private String name;
        private Set<Repository> createdRepositories$value;
        private boolean createdRepositories$set;
        private Set<Permission> permissions;
        private Role role$value;
        private boolean role$set;
        private String thirdPartyAuthType;
        private String thirdPartyAuthId;
        private String resetPasswordCode;

        UserAccountBuilder() {
        }

        public UserAccount.UserAccountBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserAccount.UserAccountBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserAccount.UserAccountBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserAccount.UserAccountBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserAccount.UserAccountBuilder createdRepositories(Set<Repository> createdRepositories) {
            this.createdRepositories$value = createdRepositories;
            this.createdRepositories$set = true;
            return this;
        }

        public UserAccount.UserAccountBuilder permissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        public UserAccount.UserAccountBuilder role(Role role) {
            this.role$value = role;
            this.role$set = true;
            return this;
        }

        public UserAccount.UserAccountBuilder thirdPartyAuthType(String thirdPartyAuthType) {
            this.thirdPartyAuthType = thirdPartyAuthType;
            return this;
        }

        public UserAccount.UserAccountBuilder thirdPartyAuthId(String thirdPartyAuthId) {
            this.thirdPartyAuthId = thirdPartyAuthId;
            return this;
        }

        public UserAccount.UserAccountBuilder resetPasswordCode(String resetPasswordCode) {
            this.resetPasswordCode = resetPasswordCode;
            return this;
        }

        public UserAccount build() {
            Set<Repository> createdRepositories$value = this.createdRepositories$value;
            if (!this.createdRepositories$set) {
                createdRepositories$value = UserAccount.$default$createdRepositories();
            }
            Role role$value = this.role$value;
            if (!this.role$set) {
                role$value = UserAccount.$default$role();
            }
            return new UserAccount(id, username, password, name, createdRepositories$value, permissions, role$value, thirdPartyAuthType, thirdPartyAuthId, resetPasswordCode);
        }

        public String toString() {
            return "UserAccount.UserAccountBuilder(id=" + this.id + ", username=" + this.username + ", password=" + this.password + ", name=" + this.name + ", createdRepositories$value=" + this.createdRepositories$value + ", permissions=" + this.permissions + ", role$value=" + this.role$value + ", thirdPartyAuthType=" + this.thirdPartyAuthType + ", thirdPartyAuthId=" + this.thirdPartyAuthId + ", resetPasswordCode=" + this.resetPasswordCode + ")";
        }
    }
}
