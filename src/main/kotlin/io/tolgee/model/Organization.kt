package io.tolgee.model

import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size


@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["address_part"], name = "organization_address_part_unique")])
data class Organization(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @field:NotBlank @field:Size(min = 3, max = 50)
        var name: String? = null,

        var description: String? = null,

        @Column(name = "address_part")
        @field:NotBlank @field:Size(min = 3, max = 60)
        var addressPart: String? = null,

        @Enumerated(EnumType.STRING)
        var basePermissions: Permission.RepositoryPermissionType = Permission.RepositoryPermissionType.VIEW,
) {
    constructor(id: Long?,
                name: String?,
                description: String?,
                addressPart: String?,
                basePermissions: Permission.RepositoryPermissionType,
                memberRole: OrganizationMemberRole
    ) : this(id, name, description, addressPart, basePermissions) {

    }

    @OneToMany(mappedBy = "organization")
    var memberRoles: List<OrganizationMemberRole> = mutableListOf()
}
