package io.tolgee.model

import javax.persistence.*

@Entity
data class OrganizationMemberRole(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long? = null,

        @ManyToOne
        var user: UserAccount? = null,

        @OneToOne
        var invitation: Invitation? = null,

        @Enumerated(EnumType.ORDINAL)
        var type: OrganizationRoleType? = null
) : AuditModel() {

    constructor(id: Long?, user: UserAccount?, invitation: Invitation?, organization: Organization?,
                type: OrganizationRoleType?) : this(id, user, invitation, type) {
        this.organization = organization
    }

    @ManyToOne
    var organization: Organization? = null
}
