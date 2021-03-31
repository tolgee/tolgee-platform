package io.tolgee.model

import io.tolgee.model.enums.OrganizationRoleType
import javax.persistence.*

@Entity
@Table(uniqueConstraints = [
    UniqueConstraint(
            columnNames = ["user_id", "organization_id"],
            name = "organization_member_role_user_organization_unique")
])
data class OrganizationMemberRole(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @OneToOne
        var invitation: Invitation? = null,

        @Enumerated(EnumType.ORDINAL)
        var type: OrganizationRoleType? = null
) : AuditModel() {

    constructor(id: Long? = null, user: UserAccount?, invitation: Invitation? = null, organization: Organization?,
                type: OrganizationRoleType?) : this(id, invitation, type) {
        this.organization = organization
        this.user = user
    }

    @ManyToOne
    var user: UserAccount? = null

    @ManyToOne
    var organization: Organization? = null
}
