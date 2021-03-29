package io.tolgee.model

import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size


@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["address_part"], name = "organization_address_part_unique")])
open class Organization(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        open var id: Long? = null,

        @field:NotBlank @field:Size(min = 3, max = 50)
        open var name: String? = null,

        open var description: String? = null,

        @Column(name = "address_part")
        @field:NotBlank @field:Size(min = 3, max = 60)
        open var addressPart: String? = null,

        @Enumerated(EnumType.STRING)
        open var basePermissions: Permission.RepositoryPermissionType = Permission.RepositoryPermissionType.VIEW,
) {
    constructor(
            name: String?,
            description: String? = null,
            addressPart: String?,
            basePermissions: Permission.RepositoryPermissionType = Permission.RepositoryPermissionType.VIEW,
    ) : this(null, name, description, addressPart, basePermissions)

    @OneToMany(mappedBy = "organization")
    open var memberRoles: MutableList<OrganizationMemberRole> = mutableListOf()
}
