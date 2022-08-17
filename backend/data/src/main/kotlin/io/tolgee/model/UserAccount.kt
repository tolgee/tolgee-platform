package io.tolgee.model

import com.vladmihalcea.hibernate.type.array.ListArrayType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.*
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
@TypeDef(name = "string-array", typeClass = ListArrayType::class)
data class UserAccount(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  override var id: Long = 0L,

  @field:NotBlank
  var username: String = "",

  var password: String? = null,

  @Column(name = "totp_key", columnDefinition = "bytea")
  var totpKey: ByteArray? = null,

  @Type(type = "string-array")
  @Column(name = "totp_recovery_codes", columnDefinition = "text[]")
  var totpRecoveryCodes: List<String> = emptyList(),

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
  @OneToMany(mappedBy = "user", orphanRemoval = true)
  var organizationRoles: MutableList<OrganizationRole> = mutableListOf()

  @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE])
  var preferences: UserPreferences? = null

  override var avatarHash: String? = null

  constructor(
    id: Long?,
    username: String?,
    password: String?,
    name: String?,
    permissions: MutableSet<Permission>?,
    role: Role = Role.USER,
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
