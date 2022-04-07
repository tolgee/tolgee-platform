package io.tolgee.model

import io.tolgee.model.enums.OrganizationRoleType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotNull

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["user_id", "organization_id"],
      name = "organization_member_role_user_organization_unique"
    )
  ]
)

data class OrganizationRole(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @OneToOne
  var invitation: Invitation? = null,

  @Enumerated(EnumType.ORDINAL)
  var type: OrganizationRoleType? = null
) : AuditModel() {

  constructor(
    id: Long? = null,
    user: UserAccount? = null,
    invitation: Invitation? = null,
    organization: Organization?,
    type: OrganizationRoleType?
  ) : this(id, invitation, type) {
    this.organization = organization
    this.user = user
  }

  @ManyToOne
  var user: UserAccount? = null

  @ManyToOne
  @NotNull
  var organization: Organization? = null
}
