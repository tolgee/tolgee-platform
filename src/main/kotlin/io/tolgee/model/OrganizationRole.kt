package io.tolgee.model

import io.tolgee.model.enums.OrganizationRoleType
import org.hibernate.envers.Audited
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [
    UniqueConstraint(
            columnNames = ["user_id", "organization_id"],
            name = "organization_member_role_user_organization_unique")
])
@Audited
data class OrganizationRole(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @OneToOne
        var invitation: Invitation? = null,

        @Enumerated(EnumType.ORDINAL)
        var type: OrganizationRoleType? = null
) : AuditModel() {

    constructor(id: Long? = null, user: UserAccount? = null, invitation: Invitation? = null, organization: Organization?,
                type: OrganizationRoleType?) : this(id, invitation, type) {
        this.organization = organization
        this.user = user
    }

    @ManyToOne
    var user: UserAccount? = null

    @ManyToOne
    @NotNull
    var organization: Organization? = null
}
