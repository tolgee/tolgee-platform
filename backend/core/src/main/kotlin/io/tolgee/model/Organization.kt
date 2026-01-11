package io.tolgee.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Filter
import java.util.Date

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["address_part"], name = "organization_address_part_unique"),
  ],
)
class Organization(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0,
  @field:NotBlank
  @field:Size(min = 1, max = 50) var name: String = "",
  var description: String? = null,
  @Column(name = "address_part")
  @field:NotBlank
  @field:Size(min = 1, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-_]*$", message = "invalid_pattern") var slug: String = "",
  @OneToOne(mappedBy = "organization", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
  var mtCreditBucket: MtCreditBucket? = null,
) : AuditModel(),
  ModelWithAvatar,
  SoftDeletable,
  EntityWithId {
  @OneToOne(mappedBy = "organization", optional = false, orphanRemoval = true, fetch = FetchType.LAZY)
  lateinit var basePermission: Permission

  @JsonIgnore
  @OneToMany(mappedBy = "organization")
  var memberRoles: MutableList<OrganizationRole> = mutableListOf()

  @OneToMany(mappedBy = "organizationOwner")
  @field:Filter(
    name = "deletedFilter",
    condition = "(deleted_at IS NULL)",
  )
  var projects: MutableList<Project> = mutableListOf()

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "organizationOwner")
  @field:Filter(
    name = "deletedFilter",
    condition = "(deleted_at IS NULL)",
  )
  var glossaries: MutableSet<Glossary> = mutableSetOf()

  @OneToMany(mappedBy = "preferredOrganization")
  var preferredBy: MutableList<UserPreferences> = mutableListOf()

  override var avatarHash: String? = null

  override var deletedAt: Date? = null

  @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
  var ssoTenant: SsoTenant? = null

  @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, orphanRemoval = true)
  var organizationSlackWorkspace: MutableList<OrganizationSlackWorkspace> = mutableListOf()

  @Transient
  override var disableActivityLogging: Boolean = false
}
