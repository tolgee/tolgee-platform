package io.tolgee.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["address_part"], name = "organization_address_part_unique"),
  ]
)
class Organization(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0,

  @field:NotBlank @field:Size(min = 3, max = 50)
  open var name: String = "",

  open var description: String? = null,

  @Column(name = "address_part")
  @field:NotBlank @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-_]*$", message = "invalid_pattern")
  open var slug: String = "",

  @OneToOne(mappedBy = "organization", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
  var mtCreditBucket: MtCreditBucket? = null
) : ModelWithAvatar, AuditModel() {

  @OneToOne(mappedBy = "organization", optional = false, orphanRemoval = true)
  lateinit var basePermission: Permission

  @JsonIgnore
  @OneToMany(mappedBy = "organization")
  var memberRoles: MutableList<OrganizationRole> = mutableListOf()

  @OneToMany(mappedBy = "organizationOwner")
  var projects: MutableList<Project> = mutableListOf()

  @OneToMany(mappedBy = "preferredOrganization")
  var preferredBy: MutableList<UserPreferences> = mutableListOf()

  override var avatarHash: String? = null
}
