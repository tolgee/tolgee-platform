package io.tolgee.model

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      columnNames = ["username"],
      name = "useraccount_username"
    ),
    UniqueConstraint(
      columnNames = ["third_party_auth_type", "third_party_auth_id"],
      name = "useraccount_authtype_auth_id"
    )
  ]
)

data class UserAccount(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0L,

  @field:NotBlank
  var username: String = "",

  var password: String? = null,

  var name: String = "",
  @Enumerated(EnumType.STRING)
  var role: Role? = Role.USER
) : AuditModel(), ModelWithAvatar {

  @OneToMany(mappedBy = "user")
  var permissions: MutableSet<Permission>? = null

  @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY, optional = true)
  var emailVerification: EmailVerification? = null

  @Column(name = "third_party_auth_type")
  var thirdPartyAuthType: String? = null

  @Column(name = "third_party_auth_id")
  var thirdPartyAuthId: String? = null

  @Column(name = "reset_password_code")
  var resetPasswordCode: String? = null

  @OrderBy("id ASC")
  @OneToMany(mappedBy = "user")
  var organizationRoles: MutableList<OrganizationRole> = mutableListOf()

  @OneToOne(mappedBy = "userAccount", cascade = [CascadeType.REMOVE])
  var mtCreditBucket: MtCreditBucket? = null

  override var avatarHash: String? = null

  constructor(
    id: Long?,
    username: String?,
    password: String?,
    name: String?,
    permissions: MutableSet<Permission>?,
    role: Role?,
    thirdPartyAuthType: String?,
    thirdPartyAuthId: String?,
    resetPasswordCode: String?
  ) : this(id = 0L, username = "", password, name = "") {
    this.permissions = permissions
    this.role = role
    this.thirdPartyAuthType = thirdPartyAuthType
    this.thirdPartyAuthId = thirdPartyAuthId
    this.resetPasswordCode = resetPasswordCode
  }

  enum class Role {
    USER, ADMIN
  }
}
