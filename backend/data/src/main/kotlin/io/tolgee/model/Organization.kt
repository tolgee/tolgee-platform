package io.tolgee.model

import org.hibernate.envers.Audited
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["address_part"], name = "organization_address_part_unique"),
    UniqueConstraint(columnNames = ["third_party_billing_id"], name = "organization_third_party_billing_id_unique")
  ]
)
@Audited
class Organization(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  open var id: Long = 0,

  @field:NotBlank @field:Size(min = 3, max = 50)
  open var name: String? = null,

  open var description: String? = null,

  @Column(name = "address_part")
  @field:NotBlank @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  open var slug: String? = null,

  @Enumerated(EnumType.STRING)
  open var basePermissions: Permission.ProjectPermissionType = Permission.ProjectPermissionType.VIEW,
) {
  constructor(
    name: String?,
    description: String? = null,
    slug: String?,
    basePermissions: Permission.ProjectPermissionType = Permission.ProjectPermissionType.VIEW,
  ) : this(0, name, description, slug, basePermissions)

  @OneToMany(mappedBy = "organization")
  var memberRoles: MutableList<OrganizationRole> = mutableListOf()

  @OneToMany(mappedBy = "organizationOwner")
  var projects: MutableList<Project> = mutableListOf()

  @Column(name = "third_party_billing_id")
  var thirdPartyBillingId: String? = null
}
