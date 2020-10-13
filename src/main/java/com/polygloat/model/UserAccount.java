package com.polygloat.model;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username"}, name = "useraccount_username"),
        @UniqueConstraint(columnNames = {"third_party_auth_type", "third_party_auth_id"}, name = "useraccount_authtype_auth_id"),

})
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Builder
@AllArgsConstructor
@Data
@ToString(of = {"id", "username", "name"})
public class UserAccount extends AuditModel {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String name;

    @OneToMany(mappedBy = "createdBy")
    @Builder.Default
    private Set<Repository> createdRepositories = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Permission> permissions;

    @Builder.Default
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

    public enum Role {
        USER,
        ADMIN
    }
}
