package io.tolgee.model

import io.tolgee.model.enums.OrganizationRoleType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
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
class OrganizationRole(
  @OneToOne
  var invitation: Invitation? = null,

  @Enumerated(EnumType.ORDINAL)
  var type: OrganizationRoleType? = null
) : StandardAuditModel() {

  constructor(
    user: UserAccount? = null,
    invitation: Invitation? = null,
    organization: Organization?,
    type: OrganizationRoleType?
  ) : this(invitation, type) {
    this.organization = organization
    this.user = user
  }

  @ManyToOne
  var user: UserAccount? = null

  @ManyToOne
  @NotNull
  var organization: Organization? = null
}
