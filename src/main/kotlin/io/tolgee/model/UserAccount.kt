package io.tolgee.model

import javax.persistence.*

@Entity
@Table(
        uniqueConstraints = [
            UniqueConstraint(
                    columnNames = ["username"],
                    name = "useraccount_username"
            ),
            UniqueConstraint(
                    columnNames = ["third_party_auth_type", "third_party_auth_id"],
                    name = "useraccount_authtype_auth_id")
        ]
)
data class UserAccount(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,
        var username: String? = null,
        var password: String? = null,
        var name: String? = null,
        @Enumerated(EnumType.STRING)
        var role: Role? = Role.USER
) : AuditModel() {

    @OneToMany(mappedBy = "user")
    var permissions: MutableSet<Permission>? = null

    @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY, optional = true)
    var emailVerification: EmailVerification? = null

    @Column(name = "third_party_auth_type")
    var thirdPartyAuthType: String? = null

    @Column(name = "third_party_auth_id")
    var thirdPartyAuthId: String? = null

    @Column(name = "reset_password_code")
    var resetPasswordCode: String? = null

    @OneToMany(mappedBy = "user")
    var organizationRoles: MutableList<OrganizationRole> = mutableListOf()

    constructor(id: Long?,
                username: String?,
                password: String?,
                name: String?,
                permissions: MutableSet<Permission>?,
                role: Role?,
                thirdPartyAuthType: String?,
                thirdPartyAuthId: String?,
                resetPasswordCode: String?
    ) : this(id, username, password, name) {
        this.permissions = permissions
        this.role = role
        this.thirdPartyAuthType = thirdPartyAuthType
        this.thirdPartyAuthId = thirdPartyAuthId
        this.resetPasswordCode = resetPasswordCode
    }

    enum class Role {
        USER, ADMIN
    }
}
