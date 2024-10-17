package io.tolgee.model

import io.tolgee.model.enums.OrganizationRoleType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["user_id", "organization_id"],
      name = "organization_member_role_user_organization_unique",
    ),
  ],
)
class OrganizationRole(
  @OneToOne
  var invitation: Invitation? = null,
  @Enumerated(EnumType.ORDINAL)
  var type: OrganizationRoleType,
) : StandardAuditModel() {
  constructor(
    user: UserAccount? = null,
    invitation: Invitation? = null,
    organization: Organization?,
    type: OrganizationRoleType,
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
